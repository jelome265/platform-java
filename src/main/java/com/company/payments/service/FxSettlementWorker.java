package com.company.payments.service;

import com.company.payments.domain.FxOffer;
import com.company.payments.domain.SettlementJob;
import com.company.payments.repository.FxOfferRepository;
import com.company.payments.repository.SettlementJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the lifecycle of FX settlement jobs.
 * Implements the "wait for confirmation -> commit" or "timeout -> revert" pattern.
 */
@Service
public class FxSettlementWorker {

    private static final Logger log = LoggerFactory.getLogger(FxSettlementWorker.class);

    private final SettlementJobRepository settlementJobRepository;
    private final FxOfferRepository fxOfferRepository;
    private final LedgerService ledgerService;
    private final AuditService auditService;

    public FxSettlementWorker(SettlementJobRepository settlementJobRepository,
                             FxOfferRepository fxOfferRepository,
                             LedgerService ledgerService,
                             AuditService auditService) {
        this.settlementJobRepository = settlementJobRepository;
        this.fxOfferRepository = fxOfferRepository;
        this.ledgerService = ledgerService;
        this.auditService = auditService;
    }

    /**
     * Periodically scan for timed-out settlement jobs and trigger compensating transactions.
     */
    @Scheduled(fixedDelay = 60000) // Every 1 minute
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processTimedOutJobs() {
        List<SettlementJob> staleJobs = settlementJobRepository.findByStatusAndExpiresAtBefore("PENDING", ZonedDateTime.now());

        for (SettlementJob job : staleJobs) {
            log.warn("[SETTLEMENT-WORKER] Reverting timed-out job: {}", job.getId());
            revertSettlement(job);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void revertSettlement(SettlementJob job) {
        FxOffer offer = fxOfferRepository.findById(job.getOfferId())
                .orElseThrow(() -> new IllegalStateException("Offer not found for job: " + job.getId()));

        // Compensating entries: release reservations for both parties
        ledgerService.recordEntry(offer.getSellerWalletId(), UUID.randomUUID(), "FX_SETTLEMENT_REVERT",
                offer.getSellAmount(), offer.getSellCurrency(),
                Map.of("offer_id", offer.getId().toString(), "job_id", job.getId().toString(), "side", "seller"));

        if (offer.getBuyerWalletId() != null) {
            ledgerService.recordEntry(offer.getBuyerWalletId(), UUID.randomUUID(), "FX_SETTLEMENT_REVERT",
                    offer.getBuyAmount(), offer.getBuyCurrency(),
                    Map.of("offer_id", offer.getId().toString(), "job_id", job.getId().toString(), "side", "buyer"));
        }

        job.setStatus("TIMED_OUT");
        offer.setStatus("OPEN"); // Put back on the book or mark failed
        
        settlementJobRepository.save(job);
        fxOfferRepository.save(offer);
        
        auditService.log(offer.getSellerId(), "FX_SETTLEMENT_TIMED_OUT", "Trade failed to settle, funds reverted");
    }
}

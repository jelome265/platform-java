package com.company.payments.service;

import com.company.payments.domain.FxOffer;
import com.company.payments.domain.SettlementJob;
import com.company.payments.domain.Wallet;
import com.company.payments.repository.FxOfferRepository;
import com.company.payments.repository.SettlementJobRepository;
import com.company.payments.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FX Escrow Service — MVP escrow model for P2P FX trades.
 * 
 * Flow:
 *   1. Seller creates FX offer → seller's funds locked in escrow (RESERVED ledger entry).
 *   2. Buyer accepts offer → buyer's funds locked in escrow (RESERVED ledger entry).
 *   3. Both funds locked → atomic settlement:
 *        - Seller's sell-currency debited, buyer receives it.
 *        - Buyer's buy-currency debited, seller receives it.
 *   4. If timeout or cancellation → compensating entries revert the reservations.
 */
@Service
public class FxEscrowService {

    private static final Logger log = LoggerFactory.getLogger(FxEscrowService.class);
    private static final int SETTLEMENT_TIMEOUT_MINUTES = 30;

    private final FxOfferRepository fxOfferRepository;
    private final WalletRepository walletRepository;
    private final SettlementJobRepository settlementJobRepository;
    private final LedgerService ledgerService;
    private final AuditService auditService;

    public FxEscrowService(FxOfferRepository fxOfferRepository, WalletRepository walletRepository,
                           SettlementJobRepository settlementJobRepository,
                           LedgerService ledgerService, AuditService auditService) {
        this.fxOfferRepository = fxOfferRepository;
        this.walletRepository = walletRepository;
        this.settlementJobRepository = settlementJobRepository;
        this.ledgerService = ledgerService;
        this.auditService = auditService;
    }

    /**
     * Step 1: Create an FX offer and lock seller's funds in escrow.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FxOffer createOffer(UUID sellerId, UUID sellerWalletId, String sellCurrency,
                                BigDecimal sellAmount, String buyCurrency, BigDecimal exchangeRate) {

        BigDecimal buyAmount = sellAmount.multiply(exchangeRate);

        FxOffer offer = new FxOffer(
                UUID.randomUUID(), sellerId, sellerWalletId,
                sellCurrency, sellAmount, buyCurrency, buyAmount, exchangeRate
        );

        // Lock seller's funds via RESERVED ledger entry
        UUID escrowTxId = UUID.randomUUID();
        ledgerService.recordEntry(sellerWalletId, escrowTxId, "FX_ESCROW_RESERVED",
                sellAmount.negate(), sellCurrency,
                Map.of("offer_id", offer.getId().toString(), "side", "seller"));

        fxOfferRepository.save(offer);
        auditService.log(sellerId, "FX_OFFER_CREATED",
                String.format("Sell %s %s for %s at rate %s", sellAmount, sellCurrency, buyCurrency, exchangeRate));

        log.info("FX offer created: {} | {} {} -> {} {}", offer.getId(), sellAmount, sellCurrency, buyAmount, buyCurrency);
        return offer;
    }

    /**
     * Step 2 & 3: Buyer accepts an offer → lock buyer's funds → atomic settlement.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FxOffer acceptOffer(UUID offerId, UUID buyerId, UUID buyerWalletId) {
        FxOffer offer = fxOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));

        if (!"OPEN".equals(offer.getStatus())) {
            throw new IllegalStateException("Offer is not available. Status: " + offer.getStatus());
        }

        offer.setBuyerId(buyerId);
        offer.setBuyerWalletId(buyerWalletId);
        offer.setStatus("SETTLING");

        // Lock buyer's buy-currency funds
        UUID buyerEscrowTxId = UUID.randomUUID();
        ledgerService.recordEntry(buyerWalletId, buyerEscrowTxId, "FX_ESCROW_RESERVED",
                offer.getBuyAmount().negate(), offer.getBuyCurrency(),
                Map.of("offer_id", offer.getId().toString(), "side", "buyer"));

        // Phase 2: Create Settlement Job for atomic tracking
        SettlementJob job = new SettlementJob(UUID.randomUUID(), offer.getId(), SETTLEMENT_TIMEOUT_MINUTES);
        settlementJobRepository.save(job);

        // === ATOMIC SETTLEMENT ===
        // 1. Prepare: Seller receives buyer's currency
        UUID sellerReceiveTxId = UUID.randomUUID();
        ledgerService.recordEntry(offer.getSellerWalletId(), sellerReceiveTxId, "FX_SETTLEMENT_CREDIT",
                offer.getBuyAmount(), offer.getBuyCurrency(),
                Map.of("offer_id", offer.getId().toString(), "settlement", "seller_receives", "job_id", job.getId().toString()));

        // 2. Prepare: Buyer receives seller's currency
        UUID buyerReceiveTxId = UUID.randomUUID();
        ledgerService.recordEntry(buyerWalletId, buyerReceiveTxId, "FX_SETTLEMENT_CREDIT",
                offer.getSellAmount(), offer.getSellCurrency(),
                Map.of("offer_id", offer.getId().toString(), "settlement", "buyer_receives", "job_id", job.getId().toString()));

        job.setStatus("COMMITTED");
        offer.setStatus("COMPLETED");
        offer.setSettledAt(ZonedDateTime.now());
        
        settlementJobRepository.save(job);
        fxOfferRepository.save(offer);

        auditService.log(buyerId, "FX_OFFER_ACCEPTED",
                String.format("Accepted offer %s: bought %s %s at rate %s",
                        offerId, offer.getSellAmount(), offer.getSellCurrency(), offer.getExchangeRate()));

        log.info("FX trade settled: offerId={}", offerId);
        return offer;
    }

    /**
     * Cancel an open offer and revert the escrow reservation.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelOffer(UUID offerId, UUID requesterId) {
        FxOffer offer = fxOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));

        if (!"OPEN".equals(offer.getStatus())) {
            throw new IllegalStateException("Cannot cancel offer in status: " + offer.getStatus());
        }

        if (!offer.getSellerId().equals(requesterId)) {
            throw new IllegalArgumentException("Only the seller can cancel their own offer");
        }

        // Revert escrow: compensating entry to release seller's funds
        UUID revertTxId = UUID.randomUUID();
        ledgerService.recordEntry(offer.getSellerWalletId(), revertTxId, "FX_ESCROW_CANCELLED",
                offer.getSellAmount(), offer.getSellCurrency(),
                Map.of("offer_id", offer.getId().toString(), "action", "cancellation_reversal"));

        offer.setStatus("CANCELLED");
        fxOfferRepository.save(offer);

        auditService.log(requesterId, "FX_OFFER_CANCELLED", "Offer " + offerId + " cancelled");
        log.info("FX offer cancelled and escrow reverted: offerId={}", offerId);
    }

    /**
     * List available open offers for a given currency pair.
     */
    public List<FxOffer> getOpenOffers(String sellCurrency, String buyCurrency) {
        return fxOfferRepository.findBySellCurrencyAndBuyCurrencyAndStatus(sellCurrency, buyCurrency, "OPEN");
    }
}

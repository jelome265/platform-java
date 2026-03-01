package com.company.payments.service;

import com.company.payments.domain.Card;
import com.company.payments.domain.User;
import com.company.payments.domain.Wallet;
import com.company.payments.repository.CardRepository;
import com.company.payments.repository.UserRepository;
import com.company.payments.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Card Issuance Service.
 * Integrates with BIN sponsor (e.g. Marqeta) for virtual card issuance.
 * NEVER stores PAN/CVV — only token + last4 + expiry returned by the sponsor.
 * 
 * Authorization flow:
 *   1. Auth webhook from BIN sponsor → create RESERVED ledger entry.
 *   2. Capture webhook → convert RESERVED → CAPTURED ledger entry.
 *   3. Chargeback webhook → create reversing entries + allocate to chargeback reserve.
 */
@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;
    private final AuditService auditService;

    public CardService(CardRepository cardRepository, UserRepository userRepository,
                       WalletRepository walletRepository, LedgerService ledgerService,
                       AuditService auditService) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.ledgerService = ledgerService;
        this.auditService = auditService;
    }

    /**
     * Issue a new virtual card via BIN sponsor.
     * In production: calls Marqeta POST /cards API over mTLS.
     * Returns token + last4 + expiry. PAN never leaves issuer.
     */
    @Transactional
    public Card issueCard(UUID userId, String binProfile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Enforce KYC requirement for card issuance
        if (!"BASIC".equals(user.getKycLevel()) && !"ENHANCED".equals(user.getKycLevel())) {
            throw new IllegalStateException("KYC verification required before card issuance. Current level: " + user.getKycLevel());
        }

        List<Wallet> wallets = walletRepository.findByUserId(userId);
        if (wallets.isEmpty()) {
            throw new IllegalStateException("No wallet found for user");
        }
        Wallet wallet = wallets.get(0);

        // --- BIN Sponsor API Call (simulated) ---
        // In production: POST https://sandbox-api.marqeta.com/v3/cards 
        //   Body: { user_token, card_product_token }
        //   Response: { token, last_four, expiration, pan (NEVER stored) }
        // Use mTLS and rotate certs quarterly.
        String cardToken = "tok_" + UUID.randomUUID().toString().substring(0, 12);
        String last4 = String.valueOf(1000 + (int)(Math.random() * 9000));
        String expiryMonth = "12";
        String expiryYear = "2028";
        // --- End Simulation ---

        Card card = new Card(UUID.randomUUID(), userId, wallet.getId(), cardToken, last4, expiryMonth, expiryYear, binProfile);
        cardRepository.save(card);

        auditService.log(userId, "CARD_ISSUED", "Virtual card issued with token: " + cardToken);
        log.info("Card issued for user {}: token={}, last4={}", userId, cardToken, last4);

        return card;
    }

    /**
     * Process authorization hold from BIN sponsor webhook.
     * Creates a RESERVED ledger entry to hold funds.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processAuthorization(String cardToken, UUID txId, BigDecimal amount, String currency, Object metadata) {
        Card card = cardRepository.findByCardToken(cardToken)
                .orElseThrow(() -> new IllegalArgumentException("Card not found for token: " + cardToken));

        // Create reservation ledger entry (funds held but not yet captured)
        ledgerService.recordEntry(card.getWalletId(), txId, "RESERVED", amount.negate(), currency, metadata);

        auditService.log(card.getUserId(), "CARD_AUTH", "Authorization hold for " + amount + " " + currency);
        log.info("Authorization processed: cardToken={}, txId={}, amount={}", cardToken, txId, amount);
    }

    /**
     * Process capture from BIN sponsor webhook.
     * Converts the reservation into a final capture entry.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processCapture(String cardToken, UUID authTxId, UUID captureTxId, BigDecimal amount, String currency, Object metadata) {
        Card card = cardRepository.findByCardToken(cardToken)
                .orElseThrow(() -> new IllegalArgumentException("Card not found for token: " + cardToken));

        // Reverse the reservation
        ledgerService.recordEntry(card.getWalletId(), UUID.randomUUID(), "RESERVATION_REVERSAL", amount, currency, metadata);

        // Create the final capture entry 
        ledgerService.recordEntry(card.getWalletId(), captureTxId, "CAPTURED", amount.negate(), currency, metadata);

        auditService.log(card.getUserId(), "CARD_CAPTURE", "Capture completed for " + amount + " " + currency);
        log.info("Capture processed: cardToken={}, authTxId={}, captureTxId={}", cardToken, authTxId, captureTxId);
    }

    /**
     * Process chargeback from BIN sponsor webhook.
     * Creates reversing entries and allocates to chargeback reserve pool.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processChargeback(String cardToken, UUID originalTxId, UUID chargebackTxId, BigDecimal amount, String currency, String reason) {
        Card card = cardRepository.findByCardToken(cardToken)
                .orElseThrow(() -> new IllegalArgumentException("Card not found for token: " + cardToken));

        // 1. Reverse the original capture (credit back to user's wallet)
        ledgerService.recordEntry(card.getWalletId(), chargebackTxId, "CHARGEBACK_REVERSAL", amount, currency,
                java.util.Map.of("original_tx_id", originalTxId.toString(), "reason", reason));

        // 2. Debit from the chargeback reserve pool (platform operational wallet)
        // In production: use a dedicated CHARGEBACK_RESERVE wallet ID
        UUID chargebackReserveWalletId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        ledgerService.recordEntry(chargebackReserveWalletId, UUID.randomUUID(), "CHARGEBACK_RESERVE_DEBIT", amount.negate(), currency,
                java.util.Map.of("chargeback_tx_id", chargebackTxId.toString()));

        auditService.log(card.getUserId(), "CARD_CHARGEBACK", reason + " | Amount: " + amount + " " + currency);
        log.info("Chargeback processed: cardToken={}, amount={}, reason={}", cardToken, amount, reason);
    }
}

package com.company.payments.controller;

import com.company.payments.domain.Card;
import com.company.payments.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for card issuance and BIN sponsor auth/capture/chargeback webhooks.
 */
@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/issue")
    public ResponseEntity<?> issueCard(@RequestBody Map<String, String> body) {
        try {
            UUID userId = UUID.fromString(body.get("user_id"));
            String binProfile = body.getOrDefault("bin_profile", "default");

            Card card = cardService.issueCard(userId, binProfile);

            return ResponseEntity.ok(Map.of(
                    "card_id", card.getId().toString(),
                    "card_token", card.getCardToken(),
                    "last4", card.getLast4(),
                    "expiry", card.getExpiryMonth() + "/" + card.getExpiryYear(),
                    "status", card.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * BIN sponsor authorization webhook.
     */
    @PostMapping("/webhooks/authorization")
    public ResponseEntity<?> handleAuthorization(@RequestBody Map<String, Object> payload) {
        try {
            String cardToken = (String) payload.get("card_token");
            UUID txId = UUID.fromString((String) payload.get("tx_id"));
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String currency = (String) payload.get("currency");

            cardService.processAuthorization(cardToken, txId, amount, currency, payload);
            return ResponseEntity.ok(Map.of("status", "authorized"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * BIN sponsor capture webhook.
     */
    @PostMapping("/webhooks/capture")
    public ResponseEntity<?> handleCapture(@RequestBody Map<String, Object> payload) {
        try {
            String cardToken = (String) payload.get("card_token");
            UUID authTxId = UUID.fromString((String) payload.get("auth_tx_id"));
            UUID captureTxId = UUID.fromString((String) payload.get("capture_tx_id"));
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String currency = (String) payload.get("currency");

            cardService.processCapture(cardToken, authTxId, captureTxId, amount, currency, payload);
            return ResponseEntity.ok(Map.of("status", "captured"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * BIN sponsor chargeback webhook.
     */
    @PostMapping("/webhooks/chargeback")
    public ResponseEntity<?> handleChargeback(@RequestBody Map<String, Object> payload) {
        try {
            String cardToken = (String) payload.get("card_token");
            UUID originalTxId = UUID.fromString((String) payload.get("original_tx_id"));
            UUID chargebackTxId = UUID.fromString((String) payload.get("chargeback_tx_id"));
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String currency = (String) payload.get("currency");
            String reason = (String) payload.getOrDefault("reason", "Chargeback initiated");

            cardService.processChargeback(cardToken, originalTxId, chargebackTxId, amount, currency, reason);
            return ResponseEntity.ok(Map.of("status", "chargeback_processed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

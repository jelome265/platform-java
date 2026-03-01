package com.company.payments.controller;

import com.company.payments.domain.FxOffer;
import com.company.payments.service.FxEscrowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/fx")
public class FxController {

    private final FxEscrowService fxEscrowService;

    public FxController(FxEscrowService fxEscrowService) {
        this.fxEscrowService = fxEscrowService;
    }

    @PostMapping("/offers")
    public ResponseEntity<?> createOffer(@RequestBody Map<String, String> body) {
        try {
            UUID sellerId = UUID.fromString(body.get("seller_id"));
            UUID sellerWalletId = UUID.fromString(body.get("seller_wallet_id"));
            String sellCurrency = body.get("sell_currency");
            BigDecimal sellAmount = new BigDecimal(body.get("sell_amount"));
            String buyCurrency = body.get("buy_currency");
            BigDecimal exchangeRate = new BigDecimal(body.get("exchange_rate"));

            FxOffer offer = fxEscrowService.createOffer(sellerId, sellerWalletId, sellCurrency, sellAmount, buyCurrency, exchangeRate);

            return ResponseEntity.ok(Map.of(
                    "offer_id", offer.getId().toString(),
                    "status", offer.getStatus(),
                    "sell", offer.getSellAmount() + " " + offer.getSellCurrency(),
                    "buy", offer.getBuyAmount() + " " + offer.getBuyCurrency(),
                    "rate", offer.getExchangeRate().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/offers/{offerId}/accept")
    public ResponseEntity<?> acceptOffer(@PathVariable UUID offerId, @RequestBody Map<String, String> body) {
        try {
            UUID buyerId = UUID.fromString(body.get("buyer_id"));
            UUID buyerWalletId = UUID.fromString(body.get("buyer_wallet_id"));

            FxOffer offer = fxEscrowService.acceptOffer(offerId, buyerId, buyerWalletId);

            return ResponseEntity.ok(Map.of(
                    "offer_id", offer.getId().toString(),
                    "status", offer.getStatus(),
                    "settled_at", offer.getSettledAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/offers/{offerId}/cancel")
    public ResponseEntity<?> cancelOffer(@PathVariable UUID offerId, @RequestBody Map<String, String> body) {
        try {
            UUID requesterId = UUID.fromString(body.get("requester_id"));
            fxEscrowService.cancelOffer(offerId, requesterId);
            return ResponseEntity.ok(Map.of("status", "cancelled"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/offers")
    public ResponseEntity<?> listOffers(@RequestParam String sellCurrency, @RequestParam String buyCurrency) {
        List<FxOffer> offers = fxEscrowService.getOpenOffers(sellCurrency, buyCurrency);
        List<Map<String, String>> result = offers.stream().map(o -> Map.of(
                "offer_id", o.getId().toString(),
                "sell", o.getSellAmount() + " " + o.getSellCurrency(),
                "buy", o.getBuyAmount() + " " + o.getBuyCurrency(),
                "rate", o.getExchangeRate().toString()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}

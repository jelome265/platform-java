package com.company.payments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * FX Offer entity for the P2P marketplace.
 * Escrow model: seller locks funds when creating offer.
 * Buyer accepts → buyer's funds locked → atomic settlement.
 */
@Table("fx_offers")
public class FxOffer {

    @Id
    private UUID id;
    private UUID sellerId;
    private UUID sellerWalletId;
    private UUID buyerId;
    private UUID buyerWalletId;
    private String sellCurrency;    // e.g. "USD"
    private String buyCurrency;     // e.g. "MWK"
    private BigDecimal sellAmount;
    private BigDecimal buyAmount;
    private BigDecimal exchangeRate;
    private String status;          // OPEN, MATCHED, SETTLING, COMPLETED, CANCELLED, EXPIRED
    private ZonedDateTime createdAt;
    private ZonedDateTime settledAt;

    public FxOffer() {}

    public FxOffer(UUID id, UUID sellerId, UUID sellerWalletId, String sellCurrency, BigDecimal sellAmount,
                   String buyCurrency, BigDecimal buyAmount, BigDecimal exchangeRate) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerWalletId = sellerWalletId;
        this.sellCurrency = sellCurrency;
        this.sellAmount = sellAmount;
        this.buyCurrency = buyCurrency;
        this.buyAmount = buyAmount;
        this.exchangeRate = exchangeRate;
        this.status = "OPEN";
        this.createdAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getSellerId() { return sellerId; }
    public UUID getSellerWalletId() { return sellerWalletId; }
    public UUID getBuyerId() { return buyerId; }
    public void setBuyerId(UUID buyerId) { this.buyerId = buyerId; }
    public UUID getBuyerWalletId() { return buyerWalletId; }
    public void setBuyerWalletId(UUID w) { this.buyerWalletId = w; }
    public String getSellCurrency() { return sellCurrency; }
    public String getBuyCurrency() { return buyCurrency; }
    public BigDecimal getSellAmount() { return sellAmount; }
    public BigDecimal getBuyAmount() { return buyAmount; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(ZonedDateTime t) { this.settledAt = t; }
}

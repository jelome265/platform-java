package com.company.payments.repository;

import com.company.payments.domain.FxOffer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FxOfferRepository extends CrudRepository<FxOffer, UUID> {
    List<FxOffer> findByStatus(String status);
    List<FxOffer> findBySellCurrencyAndBuyCurrencyAndStatus(String sellCurrency, String buyCurrency, String status);
    List<FxOffer> findBySellerId(UUID sellerId);
}

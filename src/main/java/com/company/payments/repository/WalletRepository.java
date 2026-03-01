package com.company.payments.repository;

import com.company.payments.domain.Wallet;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WalletRepository extends CrudRepository<Wallet, UUID> {
    List<Wallet> findByUserId(UUID userId);
}

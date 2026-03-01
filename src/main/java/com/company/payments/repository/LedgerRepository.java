package com.company.payments.repository;

import com.company.payments.domain.LedgerEntry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerRepository extends CrudRepository<LedgerEntry, UUID> {
    
    // Idempotency: find if a transaction already produced a ledger entry (e.g., webhook deduplication)
    Optional<LedgerEntry> findByTxId(UUID txId);
}

package com.company.payments.repository;

import com.company.payments.domain.SettlementJob;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementJobRepository extends CrudRepository<SettlementJob, UUID> {
    List<SettlementJob> findByStatusAndExpiresAtBefore(String status, ZonedDateTime now);
}

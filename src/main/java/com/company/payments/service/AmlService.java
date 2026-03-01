package com.company.payments.service;

import com.company.payments.domain.AmlAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * AML Monitoring & Rules Engine.
 * Watches for: velocity anomalies, unusual destinations, high FX conversions, repeated KYC failures.
 * Creates alerts for human compliance review with automatic SAR generation template.
 */
@Service
public class AmlService {

    private static final Logger log = LoggerFactory.getLogger(AmlService.class);
    private final AuditService auditService;

    // Thresholds (configurable in production via Vault/config)
    private static final int MAX_TRANSACTIONS_PER_HOUR = 20;
    private static final double HIGH_FX_THRESHOLD_MWK = 5_000_000.0; // 5M MWK
    private static final int MAX_KYC_FAILURES = 3;

    public AmlService(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Check velocity: too many transactions in a short period.
     */
    public void checkVelocity(UUID userId, int recentTxCount) {
        if (recentTxCount > MAX_TRANSACTIONS_PER_HOUR) {
            AmlAlert alert = new AmlAlert(
                    UUID.randomUUID(), userId, "VELOCITY", "HIGH",
                    String.format("User had %d transactions in the last hour (threshold: %d)", recentTxCount, MAX_TRANSACTIONS_PER_HOUR)
            );
            persistAlert(alert);
            auditService.log(userId, "AML_VELOCITY_ALERT", "Velocity threshold exceeded");
        }
    }

    /**
     * Check high FX conversion volumes.
     */
    public void checkHighFxConversion(UUID userId, double totalFxVolumeMwk) {
        if (totalFxVolumeMwk > HIGH_FX_THRESHOLD_MWK) {
            AmlAlert alert = new AmlAlert(
                    UUID.randomUUID(), userId, "HIGH_FX", "CRITICAL",
                    String.format("FX conversion volume %.2f MWK exceeds threshold %.2f", totalFxVolumeMwk, HIGH_FX_THRESHOLD_MWK)
            );
            persistAlert(alert);
            auditService.log(userId, "AML_HIGH_FX_ALERT", "High FX volume detected");
        }
    }

    /**
     * Check for unusual destination patterns.
     */
    public void checkUnusualDestination(UUID userId, String destinationCountry) {
        // In production: maintain a whitelist of expected countries; flag anything outside
        // For now, flag any non-MW destination
        if (!"MW".equals(destinationCountry)) {
            AmlAlert alert = new AmlAlert(
                    UUID.randomUUID(), userId, "UNUSUAL_DESTINATION", "MEDIUM",
                    "Transaction to non-domestic destination: " + destinationCountry
            );
            persistAlert(alert);
            auditService.log(userId, "AML_UNUSUAL_DEST_ALERT", "Non-domestic destination: " + destinationCountry);
        }
    }

    /**
     * Check for repeated KYC failures — strong indicator of identity fraud.
     */
    public void checkRepeatedKycFailures(UUID userId) {
        // In production: query kyc_verifications table WHERE userId AND status=REJECTED, count >= MAX
        // Simplified: always create alert on any KYC rejection for now
        AmlAlert alert = new AmlAlert(
                UUID.randomUUID(), userId, "REPEATED_KYC_FAIL", "HIGH",
                "User has experienced a KYC rejection. Review for potential identity fraud."
        );
        persistAlert(alert);
        auditService.log(userId, "AML_KYC_FAIL_ALERT", "KYC rejection triggered AML review");
    }

    /**
     * Generate SAR (Suspicious Activity Report) template.
     * In production: auto-populate fields required by RBM and export to compliance queue.
     */
    public String generateSarTemplate(UUID alertId, UUID userId) {
        return String.format(
                "=== SUSPICIOUS ACTIVITY REPORT ===\n" +
                "Alert ID: %s\n" +
                "User ID: %s\n" +
                "Filing Entity: WarmHeart Payment Services Ltd\n" +
                "Jurisdiction: Reserve Bank of Malawi\n" +
                "Date: [Auto-fill]\n" +
                "Summary: [Auto-fill from alert details]\n" +
                "Supporting Evidence: [Attach transaction logs, KYC documents]\n" +
                "Filing Officer: [Compliance Officer Name]\n" +
                "================================\n",
                alertId, userId
        );
    }

    private void persistAlert(AmlAlert alert) {
        // In production: save to aml_alerts table via AmlAlertRepository
        log.warn("[AML ALERT] type={} severity={} userId={} details={}",
                alert.getAlertType(), alert.getSeverity(), alert.getUserId(), alert.getDetails());
    }
}

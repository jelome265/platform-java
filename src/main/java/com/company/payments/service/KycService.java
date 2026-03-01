package com.company.payments.service;

import com.company.payments.domain.KycVerification;
import com.company.payments.domain.User;
import com.company.payments.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * KYC Decision Engine.
 * Levels: NONE -> BASIC -> ENHANCED -> PROHIBITED
 * 
 * BASIC: automated ID check passes (email, phone verified).
 * ENHANCED: requires manual human proof review (documents verified by ID provider).
 * PROHIBITED: watchlist hit, sanctions match, or repeated fraud signals.
 */
@Service
public class KycService {

    private static final Logger log = LoggerFactory.getLogger(KycService.class);
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AmlService amlService;

    public KycService(UserRepository userRepository, AuditService auditService, AmlService amlService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.amlService = amlService;
    }

    /**
     * Submit KYC for automated verification. 
     * In production, this calls an external ID provider (Jumio/Onfido/Trulioo) via connectors-go.
     */
    @Transactional
    public KycVerification submitKycCheck(UUID userId, String documentType, String documentRef) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        KycVerification verification = new KycVerification(UUID.randomUUID(), userId, "trulioo");
        verification.setProviderRefId(documentRef);

        // Simulate automated verification decision
        // In production: call the ID provider API via connectors-go gRPC/REST adapter
        KycDecision decision = performAutomatedCheck(user, documentType, documentRef);

        switch (decision) {
            case APPROVED_BASIC:
                verification.setStatus("APPROVED");
                verification.setResultLevel("BASIC");
                user.setKycLevel("BASIC");
                auditService.log(userId, "KYC_APPROVED_BASIC", "Automated KYC check passed");
                break;

            case APPROVED_ENHANCED:
                verification.setStatus("APPROVED");
                verification.setResultLevel("ENHANCED");
                user.setKycLevel("ENHANCED");
                auditService.log(userId, "KYC_APPROVED_ENHANCED", "Enhanced KYC with manual review passed");
                break;

            case ESCALATED:
                verification.setStatus("ESCALATED");
                verification.setResultLevel("BASIC");
                auditService.log(userId, "KYC_ESCALATED", "Requires human compliance review");
                break;

            case REJECTED:
                verification.setStatus("REJECTED");
                verification.setRejectionReason("Failed automated verification checks");
                user.setKycLevel("PROHIBITED");
                auditService.log(userId, "KYC_REJECTED", "Automated KYC check failed");
                // Trigger AML alert for repeated KYC failures
                amlService.checkRepeatedKycFailures(userId);
                break;
        }

        userRepository.save(user);
        log.info("KYC decision for user {}: {} -> level {}", userId, decision, verification.getResultLevel());

        return verification;
    }

    /**
     * Simulated automated check. In production, replace with actual ID provider API call.
     */
    private KycDecision performAutomatedCheck(User user, String documentType, String documentRef) {
        // Placeholder logic:
        // - If document type is "passport" or "national_id" and ref is non-empty -> BASIC approved
        // - If document type is "enhanced" -> ESCALATED for human review
        // - Otherwise -> REJECTED
        if (documentRef == null || documentRef.isBlank()) {
            return KycDecision.REJECTED;
        }
        if ("enhanced".equalsIgnoreCase(documentType)) {
            return KycDecision.ESCALATED;
        }
        if ("passport".equalsIgnoreCase(documentType) || "national_id".equalsIgnoreCase(documentType)) {
            return KycDecision.APPROVED_BASIC;
        }
        return KycDecision.REJECTED;
    }

    private enum KycDecision {
        APPROVED_BASIC,
        APPROVED_ENHANCED,
        ESCALATED,
        REJECTED
    }
}

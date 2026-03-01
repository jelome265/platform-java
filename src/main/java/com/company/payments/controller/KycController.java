package com.company.payments.controller;

import com.company.payments.domain.KycVerification;
import com.company.payments.service.KycService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitKyc(@RequestBody Map<String, String> body) {
        try {
            UUID userId = UUID.fromString(body.get("user_id"));
            String docType = body.get("document_type");   // passport, national_id, enhanced
            String docRef = body.get("document_ref");     // Provider reference / upload ID

            KycVerification result = kycService.submitKycCheck(userId, docType, docRef);

            return ResponseEntity.ok(Map.of(
                    "verification_id", result.getId().toString(),
                    "status", result.getStatus(),
                    "kyc_level", result.getResultLevel() != null ? result.getResultLevel() : "PENDING"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

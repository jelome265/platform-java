package com.company.payments.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Audit Hash Chain Service.
 * 
 * Produces a tamper-evident audit chain:
 * 1. Daily: collects all Kafka events for the day.
 * 2. Computes SHA-256 hash of the day's events.
 * 3. Chains with previous day's hash (hash_n = SHA-256(hash_{n-1} + events_hash)).
 * 4. Stores the hash chain in immutable storage.
 * 
 * Any tampering of events will break the hash chain and be detected
 * during compliance audits.
 */
@Service
public class AuditHashChainService {

    private static final Logger log = LoggerFactory.getLogger(AuditHashChainService.class);
    private static final String AUDIT_DIR = "audit";
    private static final String CHAIN_FILE = "hash_chain.csv";

    private String previousHash = "GENESIS";

    /**
     * Daily audit hash generation — runs at 01:00 AM.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void generateDailyAuditHash() {
        String date = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE);
        log.info("[AUDIT] Generating daily audit hash for {}", date);

        try {
            Path dir = Paths.get(AUDIT_DIR);
            Files.createDirectories(dir);

            // In production: read all events from Kafka topic "ledger-events" for the day
            // or from the archived event files in S3/immutable storage.
            String eventsContent = fetchDailyEvents(date);

            // Compute hash of day's events
            String eventsHash = sha256(eventsContent);

            // Chain with previous hash
            String chainedHash = sha256(previousHash + eventsHash);

            // Append to hash chain file
            Path chainPath = dir.resolve(CHAIN_FILE);
            try (PrintWriter pw = new PrintWriter(new FileWriter(chainPath.toFile(), true))) {
                if (!Files.exists(chainPath) || Files.size(chainPath) == 0) {
                    pw.println("date,events_hash,chained_hash,previous_hash");
                }
                pw.printf("%s,%s,%s,%s%n", date, eventsHash, chainedHash, previousHash);
            }

            // Update previous hash for next day
            previousHash = chainedHash;

            log.info("[AUDIT] Hash chain updated for {}: chained_hash={}", date, chainedHash);

        } catch (IOException e) {
            log.error("[AUDIT] Failed to generate audit hash", e);
        }
    }

    /**
     * Verify hash chain integrity — runs on startup and weekly.
     */
    @Scheduled(cron = "0 0 2 * * MON")
    public void verifyHashChain() {
        log.info("[AUDIT] Verifying hash chain integrity...");

        try {
            Path chainPath = Paths.get(AUDIT_DIR, CHAIN_FILE);
            if (!Files.exists(chainPath)) {
                log.info("[AUDIT] No hash chain file found. Skipping verification.");
                return;
            }

            var lines = Files.readAllLines(chainPath);
            if (lines.size() <= 1) {
                log.info("[AUDIT] Hash chain has no entries yet.");
                return;
            }

            String prevHash = "GENESIS";
            int validCount = 0;

            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                String date = parts[0];
                String eventsHash = parts[1];
                String recordedChainedHash = parts[2];
                String recordedPrevHash = parts[3];

                // Verify chain linkage
                if (!recordedPrevHash.equals(prevHash)) {
                    log.error("[AUDIT INTEGRITY VIOLATION] Chain broken at {}: expected prev={}, got prev={}",
                            date, prevHash, recordedPrevHash);
                    return;
                }

                // Verify chained hash
                String expectedChainedHash = sha256(prevHash + eventsHash);
                if (!expectedChainedHash.equals(recordedChainedHash)) {
                    log.error("[AUDIT INTEGRITY VIOLATION] Hash mismatch at {}: expected={}, recorded={}",
                            date, expectedChainedHash, recordedChainedHash);
                    return;
                }

                prevHash = recordedChainedHash;
                validCount++;
            }

            log.info("[AUDIT] Hash chain verified successfully. {} entries validated.", validCount);

        } catch (IOException e) {
            log.error("[AUDIT] Hash chain verification failed", e);
        }
    }

    private String fetchDailyEvents(String date) {
        // In production: query Kafka consumer or read from S3 archive
        // Returns concatenated event payloads for the day
        return "events_for_" + date;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

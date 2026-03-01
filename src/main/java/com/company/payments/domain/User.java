package com.company.payments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

@Table("users")
public class User {

    @Id
    private UUID id;
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private String kycLevel; // BASIC, ENHANCED, PROHIBITED
    private String deviceFingerprint;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public User() {}

    public User(UUID id, String email, String phoneNumber, String passwordHash) {
        this.id = id;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.kycLevel = "NONE";
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPasswordHash() { return passwordHash; }
    public String getKycLevel() { return kycLevel; }
    public void setKycLevel(String kycLevel) { this.kycLevel = kycLevel; this.updatedAt = ZonedDateTime.now(); }
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String fp) { this.deviceFingerprint = fp; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getUpdatedAt() { return updatedAt; }
}

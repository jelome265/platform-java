package com.company.payments.service;

import com.company.payments.domain.User;
import com.company.payments.domain.Wallet;
import com.company.payments.repository.UserRepository;
import com.company.payments.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, WalletRepository walletRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.auditService = auditService;
    }

    @Transactional
    public User signup(String email, String phoneNumber, String rawPassword, String deviceFingerprint) {
        // 1. Check for existing user
        Optional<User> existingEmail = userRepository.findByEmail(email);
        if (existingEmail.isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        Optional<User> existingPhone = userRepository.findByPhoneNumber(phoneNumber);
        if (existingPhone.isPresent()) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        // 2. Hash password (in production, use BCrypt; this is a simplified SHA-256 + salt)
        String passwordHash = hashPassword(rawPassword);

        // 3. Create user
        User user = new User(UUID.randomUUID(), email, phoneNumber, passwordHash);
        user.setDeviceFingerprint(deviceFingerprint);
        userRepository.save(user);

        // 4. Provision default MWK wallet
        Wallet mwkWallet = new Wallet(UUID.randomUUID(), user.getId(), "MWK");
        walletRepository.save(mwkWallet);

        // 5. Audit log
        auditService.log(user.getId(), "USER_SIGNUP", "User registered with email: " + email);

        log.info("User signed up: {} with wallet {}", user.getId(), mwkWallet.getId());
        return user;
    }

    public Optional<User> login(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        String hash = hashPassword(rawPassword);

        if (!hash.equals(user.getPasswordHash())) {
            auditService.log(user.getId(), "LOGIN_FAILED", "Invalid password attempt");
            return Optional.empty();
        }

        auditService.log(user.getId(), "LOGIN_SUCCESS", "User logged in");
        return Optional.of(user);
    }

    private String hashPassword(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

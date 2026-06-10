package com.codex.travel.ticket.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import com.codex.travel.ticket.dto.AuthResponse;
import com.codex.travel.ticket.dto.LoginRequest;
import com.codex.travel.ticket.dto.RegisterRequest;
import com.codex.travel.ticket.entity.AppUser;
import com.codex.travel.ticket.repository.AppUserRepository;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Long DEFAULT_TENANT_ID = 10001L;

    private final AppUserRepository userRepository;
    private final RedisSnapshotService redisSnapshotService;

    public AuthService(AppUserRepository userRepository, RedisSnapshotService redisSnapshotService) {
        this.userRepository = userRepository;
        this.redisSnapshotService = redisSnapshotService;
    }

    @PostConstruct
    @Transactional
    public void ensureDemoAdmin() {
        if (!userRepository.existsByEmail("admin@travel.local")) {
            AppUser user = new AppUser();
            user.setTenantId(DEFAULT_TENANT_ID);
            user.setName("系统管理员");
            user.setCompany("示例集团");
            user.setEmail("admin@travel.local");
            user.setPasswordHash(hash("admin123"));
            redisSnapshotService.writeUser(userRepository.save(user));
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("email already registered");
        }

        AppUser user = new AppUser();
        user.setTenantId(generateTenantId(email));
        user.setName(request.name().trim());
        user.setCompany(request.company().trim());
        user.setEmail(email);
        user.setPasswordHash(hash(request.password()));

        AppUser saved = userRepository.save(user);
        redisSnapshotService.writeUser(saved);
        return AuthResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("email or password is incorrect"));
        if (!user.getPasswordHash().equals(hash(request.password()))) {
            throw new IllegalArgumentException("email or password is incorrect");
        }
        redisSnapshotService.writeUser(user);
        return AuthResponse.from(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Long generateTenantId(String email) {
        return 10000L + Math.abs(email.hashCode() % 900000);
    }

    private String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}

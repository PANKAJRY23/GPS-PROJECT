package com.gpsvariant.service.auth;

import com.gpsvariant.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PasswordResetCleanupService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetCleanupService.class);
    private final PasswordResetTokenRepository repository;

    public PasswordResetCleanupService(PasswordResetTokenRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${app.password-reset.cleanup-ms:3600000}")
    @Transactional
    public void deleteExpiredTokens() {
        long deleted = repository.deleteByExpiryDateBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Deleted {} expired password reset token(s)", deleted);
        }
    }
}

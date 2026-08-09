package com.gpsvariant.service.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gpsvariant.DTO.auth.RegistrationRequest;
import com.gpsvariant.entity.PasswordResetToken;
import com.gpsvariant.entity.User;
import com.gpsvariant.repository.PasswordResetTokenRepository;
import com.gpsvariant.repository.UserRepository;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Value("${app.password-reset.expiry-minutes:15}")
    private long expiryMinutes;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    public User register(RegistrationRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public void requestPasswordReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(
                () -> new IllegalArgumentException("No account found with this email"));

        passwordResetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(expiryMinutes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(expiryDate);
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        String resetUrl = normalizedBaseUrl + "/reset-password?token=" + token;

        mailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getUsername(),
                resetUrl,
                expiryMinutes);
    }

    @Transactional(readOnly = true)
    public PasswordResetToken findValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid reset token");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("This reset link has already been used");
        }

        if (resetToken.getExpiryDate() == null ||
                !resetToken.getExpiryDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("This reset link has expired");
        }

        return resetToken;
    }

    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new IllegalArgumentException("Confirm password cannot be empty");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        PasswordResetToken resetToken = findValidToken(token);
        User user = resetToken.getUser();

        if (user == null) {
            throw new IllegalArgumentException("User associated with token not found");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setEnabled(true);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

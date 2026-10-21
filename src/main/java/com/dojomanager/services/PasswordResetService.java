package com.dojomanager.services;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.dojomanager.data.entities.authorization.PasswordResetToken;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.repositories.authorization.PasswordResetTokenRepository;
import com.dojomanager.data.repositories.dojo.DojoOwnerRepository;
import com.dojomanager.services.exceptions.BadRequestException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and consumes single-use password reset tokens.
 *
 * <p>{@link #requestReset(String)} deliberately succeeds for unknown emails so
 * the endpoint cannot be used to enumerate accounts. {@link #resetPassword}
 * rejects unknown, expired and already-used tokens with a
 * {@link BadRequestException}.</p>
 */
@Service
public class PasswordResetService {

    /** Reset links stay valid for 30 minutes. */
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final DojoOwnerRepository ownerRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String resetUrlBase;

    public PasswordResetService(DojoOwnerRepository ownerRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${app.password-reset.reset-url-base:http://localhost:4200/reset-password}") String resetUrlBase) {
        this.ownerRepository = ownerRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.resetUrlBase = resetUrlBase;
    }

    /**
     * Create (and email) a reset token for {@code email}.
     *
     * @return the link/token for the caller to surface, but only when the
     *         configured email transport is the development one; empty for
     *         unknown emails and for real transports.
     */
    @Transactional
    public Optional<ResetLink> requestReset(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        Optional<DojoOwner> maybeOwner = ownerRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (maybeOwner.isEmpty()) {
            return Optional.empty();
        }

        DojoOwner owner = maybeOwner.get();

        // Only the newest link may work: retire any outstanding tokens first.
        List<PasswordResetToken> outstanding = tokenRepository.findByOwnerAndUsedFalse(owner);
        outstanding.forEach(token -> token.setUsed(true));
        tokenRepository.saveAll(outstanding);

        String rawToken = generateToken();
        PasswordResetToken resetToken = new PasswordResetToken(
                rawToken, owner, LocalDateTime.now().plus(TOKEN_TTL));
        tokenRepository.save(resetToken);

        String resetUrl = resetUrlBase + (resetUrlBase.contains("?") ? "&" : "?") + "token=" + rawToken;
        emailService.sendPasswordResetEmail(owner.getEmail(), resetUrl);

        if (!emailService.exposesResetLink()) {
            return Optional.empty();
        }

        return Optional.of(new ResetLink(resetUrl, rawToken));
    }

    /**
     * Consume {@code rawToken} and replace the owner's password.
     *
     * @throws BadRequestException when the token is unknown, expired or used
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("A password reset token is required");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new BadRequestException("Invalid password reset token"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("This password reset link has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This password reset link has expired");
        }

        DojoOwner owner = resetToken.getOwner();
        owner.setPassword(passwordEncoder.encode(newPassword));
        ownerRepository.save(owner);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        TOKEN_RANDOM.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }

    /**
     * Development-only view of a just-sent reset link.
     */
    public record ResetLink(String resetUrl, String token) {
    }
}

package com.dojomanager.services;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Development email transport: "sends" mail by writing it to the backend log
 * instead of contacting an SMTP server.
 *
 * <p>When {@code app.password-reset.expose-reset-link} is true (the default,
 * used by the dev/local/test profiles) the last link is also kept in memory and
 * returned to the caller so the UI can render a clickable reset link. Setting
 * that property to {@code false} - as the prod profile does - suppresses both
 * the in-memory value and the API field while still logging, so production
 * response shapes never leak the token.</p>
 */
@Service
@ConditionalOnProperty(name = "app.password-reset.email-mode", havingValue = "logging", matchIfMissing = true)
public class LoggingEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingEmailService.class);

    private final boolean exposeResetLink;
    private final AtomicReference<String> lastSentResetUrl = new AtomicReference<>();

    public LoggingEmailService(
            @Value("${app.password-reset.expose-reset-link:true}") boolean exposeResetLink) {
        this.exposeResetLink = exposeResetLink;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        lastSentResetUrl.set(resetUrl);
        logger.info("Password reset email (dev logging transport) to={} link={}", toEmail, resetUrl);
    }

    @Override
    public boolean exposesResetLink() {
        return exposeResetLink;
    }

    /**
     * The most recent link sent through this transport, when exposed.
     */
    public Optional<String> getLastSentResetUrl() {
        return exposeResetLink ? Optional.ofNullable(lastSentResetUrl.get()) : Optional.empty();
    }
}

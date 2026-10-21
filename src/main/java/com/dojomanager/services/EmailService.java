package com.dojomanager.services;

/**
 * Outbound email seam.
 *
 * <p>The application currently ships a development transport
 * ({@link LoggingEmailService}). Swapping in real SMTP later means adding one
 * class that implements this interface and disabling the logging transport via
 * {@code app.password-reset.email-mode}; no caller changes are required.</p>
 */
public interface EmailService {

    /**
     * Deliver a password reset message containing {@code resetUrl}.
     *
     * @param toEmail  recipient address
     * @param resetUrl fully-qualified link the recipient can open
     */
    void sendPasswordResetEmail(String toEmail, String resetUrl);

    /**
     * Whether this transport surfaces the sent link back to the HTTP caller.
     *
     * <p>Only the development transport returns {@code true}; a real SMTP
     * transport keeps the default so the API never leaks reset links.</p>
     */
    default boolean exposesResetLink() {
        return false;
    }
}

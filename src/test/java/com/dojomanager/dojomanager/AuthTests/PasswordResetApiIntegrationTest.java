package com.dojomanager.dojomanager.AuthTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.dojomanager.data.dto.MessageResponse;
import com.dojomanager.data.dto.authorization.ForgotPasswordResponse;
import com.dojomanager.data.dto.authorization.JwtResponse;
import com.dojomanager.data.entities.authorization.PasswordResetToken;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.repositories.authorization.PasswordResetTokenRepository;
import com.dojomanager.data.repositories.dojo.DojoOwnerRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PasswordResetApiIntegrationTest {

    private static final String FORGOT_URL = "/api/auth/forgot-password";
    private static final String RESET_URL = "/api/auth/reset-password";
    private static final String LOGIN_URL = "/api/auth/login";

    private static final String OLD_PASSWORD = "old-password-123";
    private static final String NEW_PASSWORD = "new-password-456";
    private static final String GENERIC_MESSAGE =
            "If an account exists for that email, a password reset link has been sent";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DojoOwnerRepository ownerRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void resetPasswordHappyPathReplacesTheOldPassword() {
        String email = uniqueEmail("happy");
        DojoOwner owner = createOwner(email, OLD_PASSWORD);

        ResponseEntity<ForgotPasswordResponse> forgot = forgotPassword(email);
        assertEquals(HttpStatus.OK, forgot.getStatusCode());
        assertNotNull(forgot.getBody());
        String token = forgot.getBody().getToken();
        assertNotNull(token, "dev transport should surface the token");
        assertNotNull(forgot.getBody().getResetUrl());
        assertTrue(forgot.getBody().getResetUrl().contains(token));

        ResponseEntity<MessageResponse> reset = resetPassword(token, NEW_PASSWORD);
        assertEquals(HttpStatus.OK, reset.getStatusCode());
        assertNotNull(reset.getBody());
        assertEquals("Password has been reset successfully", reset.getBody().getMessage());

        // The token is single-use.
        PasswordResetToken stored = tokenRepository.findByToken(token).orElseThrow();
        assertTrue(stored.isUsed());
        assertEquals(owner.getId(), stored.getOwner().getId());

        // New password authenticates, old password no longer does.
        assertEquals(HttpStatus.OK, login(email, NEW_PASSWORD).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, login(email, OLD_PASSWORD).getStatusCode());
    }

    @Test
    void resetWithUnknownTokenReturns400WithAClearMessage() {
        ResponseEntity<Map> response = rest.postForEntity(RESET_URL,
                Map.of("token", "definitely-not-a-valid-token", "password", NEW_PASSWORD), Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid password reset token", response.getBody().get("message"));
    }

    @Test
    void resetWithExpiredTokenReturns400() {
        String email = uniqueEmail("expired");
        DojoOwner owner = createOwner(email, OLD_PASSWORD);
        PasswordResetToken expired = tokenRepository.save(new PasswordResetToken(
                "expired-" + UUID.randomUUID(), owner, LocalDateTime.now().minusMinutes(1)));

        ResponseEntity<Map> response = resetPasswordRaw(expired.getToken(), NEW_PASSWORD);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("This password reset link has expired", response.getBody().get("message"));
        assertEquals(HttpStatus.UNAUTHORIZED, login(email, NEW_PASSWORD).getStatusCode());
    }

    @Test
    void resetWithAlreadyUsedTokenReturns400() {
        String email = uniqueEmail("used");
        DojoOwner owner = createOwner(email, OLD_PASSWORD);
        PasswordResetToken used = new PasswordResetToken(
                "used-" + UUID.randomUUID(), owner, LocalDateTime.now().plusMinutes(30));
        used.setUsed(true);
        tokenRepository.save(used);

        ResponseEntity<Map> response = resetPasswordRaw(used.getToken(), NEW_PASSWORD);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("This password reset link has already been used", response.getBody().get("message"));
        assertEquals(HttpStatus.UNAUTHORIZED, login(email, NEW_PASSWORD).getStatusCode());
    }

    @Test
    void forgotPasswordForUnknownEmailLooksIdenticalToKnownEmail() {
        String email = uniqueEmail("known");
        createOwner(email, OLD_PASSWORD);

        ResponseEntity<ForgotPasswordResponse> known = forgotPassword(email);
        ResponseEntity<ForgotPasswordResponse> unknown = forgotPassword(uniqueEmail("unknown"));

        assertEquals(HttpStatus.OK, known.getStatusCode());
        assertEquals(HttpStatus.OK, unknown.getStatusCode());
        assertNotNull(known.getBody());
        assertNotNull(unknown.getBody());

        // Same message, so callers cannot tell whether the account exists.
        assertEquals(GENERIC_MESSAGE, known.getBody().getMessage());
        assertEquals(GENERIC_MESSAGE, unknown.getBody().getMessage());

        // No token is created or exposed for an unknown address.
        assertNull(unknown.getBody().getToken());
        assertNull(unknown.getBody().getResetUrl());
    }

    @Test
    void requestingANewLinkInvalidatesThePreviousOne() {
        String email = uniqueEmail("rotate");
        createOwner(email, OLD_PASSWORD);

        String firstToken = forgotPassword(email).getBody().getToken();
        String secondToken = forgotPassword(email).getBody().getToken();

        assertNotNull(firstToken);
        assertNotNull(secondToken);
        assertFalse(firstToken.equals(secondToken));
        assertTrue(tokenRepository.findByToken(firstToken).orElseThrow().isUsed());

        assertEquals(HttpStatus.BAD_REQUEST,
                resetPasswordRaw(firstToken, NEW_PASSWORD).getStatusCode());
        assertEquals(HttpStatus.OK,
                resetPasswordRaw(secondToken, NEW_PASSWORD).getStatusCode());
    }

    private DojoOwner createOwner(String email, String rawPassword) {
        return ownerRepository.save(new DojoOwner("Reset", "Owner", email, passwordEncoder.encode(rawPassword)));
    }

    private ResponseEntity<ForgotPasswordResponse> forgotPassword(String email) {
        return rest.postForEntity(FORGOT_URL, Map.of("email", email), ForgotPasswordResponse.class);
    }

    private ResponseEntity<MessageResponse> resetPassword(String token, String password) {
        return rest.postForEntity(RESET_URL, Map.of("token", token, "password", password),
                MessageResponse.class);
    }

    private ResponseEntity<Map> resetPasswordRaw(String token, String password) {
        return rest.postForEntity(RESET_URL, Map.of("token", token, "password", password), Map.class);
    }

    private ResponseEntity<JwtResponse> login(String email, String password) {
        return rest.postForEntity(LOGIN_URL, Map.of("email", email, "password", password),
                JwtResponse.class);
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}

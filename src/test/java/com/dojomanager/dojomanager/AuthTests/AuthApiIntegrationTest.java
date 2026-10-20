package com.dojomanager.dojomanager.AuthTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.dojomanager.data.dto.MessageResponse;
import com.dojomanager.data.dto.authorization.JwtResponse;
import com.dojomanager.data.entities.dojo.DojoOwner;
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
class AuthApiIntegrationTest {

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String SIGNUP_URL = "/api/auth/signup";
    private static final String PASSWORD = "auth-test-password";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DojoOwnerRepository ownerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginWithValidCredentialsReturnsJwt() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "valid-" + suffix + "@example.com";
        ownerRepository.save(new DojoOwner("Valid", "User", email, passwordEncoder.encode(PASSWORD)));

        Map<String, String> creds = Map.of("email", email, "password", PASSWORD);
        ResponseEntity<JwtResponse> response = rest.postForEntity(LOGIN_URL, creds, JwtResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
    }

    @Test
    void loginWithNonExistentEmailReturns401WithCleanMessage() {
        Map<String, String> creds = Map.of("email", "nonexistent@example.com", "password", "wrongpass");
        ResponseEntity<MessageResponse> response = rest.postForEntity(LOGIN_URL, creds, MessageResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid email or password", response.getBody().getError());
    }

    @Test
    void loginWithWrongPasswordReturns401WithCleanMessage() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "wrongpass-" + suffix + "@example.com";
        ownerRepository.save(new DojoOwner("Wrong", "Pass", email, passwordEncoder.encode(PASSWORD)));

        Map<String, String> creds = Map.of("email", email, "password", "totally-wrong-password");
        ResponseEntity<MessageResponse> response = rest.postForEntity(LOGIN_URL, creds, MessageResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid email or password", response.getBody().getError());
    }

    @Test
    void signupWithExistingEmailReturns400() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "existing-" + suffix + "@example.com";
        ownerRepository.save(new DojoOwner("Existing", "User", email, passwordEncoder.encode(PASSWORD)));

        Map<String, String> signup = Map.of(
                "firstName", "Existing",
                "lastName", "User",
                "email", email,
                "password", "newpassword123"
        );
        ResponseEntity<MessageResponse> response = rest.postForEntity(SIGNUP_URL, signup, MessageResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Error: Email already exists", response.getBody().getMessage());
    }
}

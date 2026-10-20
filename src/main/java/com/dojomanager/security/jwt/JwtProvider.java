package com.dojomanager.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.crypto.SecretKey;

import com.dojomanager.security.services.OwnerPrincipal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;

@Component
public class JwtProvider {
    @Value("${security.app.jwtSecret:}")
    private String jwtSecret;

    @Value("${security.app.jwtExpiration}")
    private int jwtExpiration;

    private SecretKey key;

    private static final Logger LOG = LogManager.getLogger(JwtProvider.class);

    @PostConstruct
    public void ensureJwtSecretConfigured() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT signing secret is not configured. Set the JWT_SECRET environment variable "
                            + "(mapped to security.app.jwtSecret) to a strong, secret value and restart "
                            + "the application. Never commit a signing secret to source control.");
        }
        this.key = deriveSigningKey(jwtSecret);
    }

    private SecretKey deriveSigningKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 32) {
                keyBytes = sha256(secret.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            keyBytes = sha256(secret.getBytes(StandardCharsets.UTF_8));
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public String generateJwtToken(Authentication authentication) {
        OwnerPrincipal ownerPrincipal = (OwnerPrincipal) authentication.getPrincipal();
        Date now = new Date();
        Date expiration = new Date(now.getTime() + (long) jwtExpiration * 1000);

        return Jwts.builder()
                .subject(ownerPrincipal.getUsername())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public boolean validateJwsToken(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            return false;
        }
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken.trim());
            return true;
        } catch (SecurityException e) {
            LOG.error("Invalid JWT signature -> Message: {}", e);
        } catch (MalformedJwtException e) {
            LOG.error("Invalid JWT token -> Message: {}", e);
        } catch (ExpiredJwtException e) {
            LOG.error("Expired JWT token -> Message: {}", e);
        } catch (UnsupportedJwtException e) {
            LOG.error("Unsupported JWT token -> Message: {}", e);
        } catch (IllegalArgumentException e) {
            LOG.error("JWT claims string is empty -> Message: {}", e);
        }

        return false;
    }

    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token.trim()).getPayload().getSubject();
    }
}

package com.dojomanager.security.jwt;

import java.util.Date;

import com.dojomanager.security.services.OwnerPrincipal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

@Component
public class JwtProvider {
    @Value("${security.app.jwtSecret}")
    private String jwtSecret;
    
    @Value("${security.app.jwtExpiration}")
    private int jwtExpiration;

    private static Logger LOG = LogManager.getLogger(JwtProvider.class);

    public String generateJwtToken(Authentication authentication) {
        OwnerPrincipal ownerPrincipal = (OwnerPrincipal) authentication.getPrincipal();

        return Jwts.builder()
        .setSubject(ownerPrincipal.getUsername())
        .setIssuedAt(new Date())
        .setExpiration(new Date(new Date().getTime() + jwtExpiration*1000))
        .signWith(SignatureAlgorithm.HS512, jwtSecret)
        .compact();
    }

    public boolean validateJwsToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            LOG.error("Invalid JWT signature -> Message: {}", e);
        }catch (MalformedJwtException e) {
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
        return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
    }
}

package com.dojomanager.data.entities.authorization;

import java.time.LocalDateTime;

import com.dojomanager.data.entities.AbstractEntity;
import com.dojomanager.data.entities.dojo.DojoOwner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single-use, expiring password reset token issued to a dojo owner.
 *
 * <p>Only the raw token is stored here (it is a high-entropy random value, not
 * a password), which is what lets the reset flow look the token up directly.
 * Tokens are invalidated either by being consumed ({@code used = true}) or by
 * a newer reset request for the same owner.</p>
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "password_reset_token")
public class PasswordResetToken extends AbstractEntity {

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private DojoOwner owner;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    public PasswordResetToken(String token, DojoOwner owner, LocalDateTime expiresAt) {
        this.token = token;
        this.owner = owner;
        this.expiresAt = expiresAt;
    }
}

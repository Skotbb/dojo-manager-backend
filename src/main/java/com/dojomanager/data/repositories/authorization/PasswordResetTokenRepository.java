package com.dojomanager.data.repositories.authorization;

import java.util.List;
import java.util.Optional;

import com.dojomanager.data.entities.authorization.PasswordResetToken;
import com.dojomanager.data.entities.dojo.DojoOwner;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByOwnerAndUsedFalse(DojoOwner owner);
}

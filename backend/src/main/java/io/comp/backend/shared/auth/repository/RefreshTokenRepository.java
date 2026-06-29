package io.comp.backend.shared.auth.repository;

import io.comp.backend.shared.auth.model.RefreshToken;
import io.comp.backend.shared.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(User user);
    
    @Modifying
    int deleteByUser(User user);
}

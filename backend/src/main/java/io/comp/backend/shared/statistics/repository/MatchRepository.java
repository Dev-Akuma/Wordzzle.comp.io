package io.comp.backend.shared.statistics.repository;

import io.comp.backend.shared.auth.model.User;
import io.comp.backend.shared.statistics.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    @Query("SELECT m FROM Match m WHERE m.player1 = :user OR m.player2 = :user ORDER BY m.createdAt DESC")
    List<Match> findUserMatchHistory(@Param("user") User user);
}

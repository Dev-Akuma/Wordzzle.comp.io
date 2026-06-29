package io.comp.backend.shared.statistics.repository;

import io.comp.backend.shared.auth.model.User;
import io.comp.backend.shared.statistics.model.Match;
import io.comp.backend.shared.statistics.model.MatchPlayerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchPlayerStateRepository extends JpaRepository<MatchPlayerState, UUID> {
    Optional<MatchPlayerState> findByMatchAndUser(Match match, User user);
    Optional<MatchPlayerState> findByMatchIdAndUserId(UUID matchId, UUID userId);
    List<MatchPlayerState> findByMatch(Match match);
}

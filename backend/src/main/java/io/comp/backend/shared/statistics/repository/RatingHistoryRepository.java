package io.comp.backend.shared.statistics.repository;

import io.comp.backend.shared.auth.model.User;
import io.comp.backend.shared.statistics.model.RatingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RatingHistoryRepository extends JpaRepository<RatingHistory, UUID> {
    List<RatingHistory> findByUserOrderByCreatedAtDesc(User user);
}

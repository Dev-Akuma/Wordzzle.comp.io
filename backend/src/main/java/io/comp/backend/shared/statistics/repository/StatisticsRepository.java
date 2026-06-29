package io.comp.backend.shared.statistics.repository;

import io.comp.backend.shared.auth.model.User;
import io.comp.backend.shared.statistics.model.Statistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatisticsRepository extends JpaRepository<Statistics, UUID> {
    Optional<Statistics> findByUser(User user);
    Optional<Statistics> findByUserId(UUID userId);
}

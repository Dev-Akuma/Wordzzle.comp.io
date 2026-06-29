package io.comp.backend.shared.statistics.model;

import io.comp.backend.shared.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rating_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", referencedColumnName = "id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private Integer ratingAfter;

    @Column(nullable = false)
    private Integer changeAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

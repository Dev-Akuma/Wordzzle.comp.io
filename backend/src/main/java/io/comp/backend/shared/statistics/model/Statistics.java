package io.comp.backend.shared.statistics.model;

import io.comp.backend.shared.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statistics {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Integer gamesPlayed = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer wins = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer losses = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer winStreak = 0;

    @Column(nullable = false)
    @Builder.Default
    private Double avgGuesses = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double avgSolveTimeSeconds = 0.0;
}

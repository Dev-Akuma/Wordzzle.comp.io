package io.comp.backend.shared.statistics.model;

import io.comp.backend.shared.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "match_player_states")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchPlayerState {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", referencedColumnName = "id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    private Integer solveTimeSeconds;

    @Column(nullable = false)
    @Builder.Default
    private Integer attemptsCount = 0;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String guesses = ""; // Comma-separated list of guessed words

    @Column(nullable = false)
    @Builder.Default
    private Integer ratingChange = 0;
}

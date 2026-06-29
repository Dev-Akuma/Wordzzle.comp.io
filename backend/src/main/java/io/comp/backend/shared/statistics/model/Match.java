package io.comp.backend.shared.statistics.model;

import io.comp.backend.shared.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @Builder.Default
    private String gameType = "WORDZZLE";

    @Column(nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, DRAW, FORFEITED

    @Column(nullable = false)
    private String hiddenWord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id", referencedColumnName = "id", nullable = false)
    private User player1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id", referencedColumnName = "id", nullable = false)
    private User player2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id", referencedColumnName = "id")
    private User winner; // Nullable if draw or unfinished

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

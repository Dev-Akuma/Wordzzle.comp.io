package io.comp.backend.shared.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchHistoryEntry {
    private String matchId;
    private String opponentName;
    private String result; // WIN, LOSS, DRAW, UNFINISHED
    private Integer solveTimeSeconds; // Nullable
    private Integer attempts;
    private Integer ratingChange;
    private LocalDateTime playedAt;
}

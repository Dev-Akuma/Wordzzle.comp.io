package io.comp.backend.shared.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    private String username;
    private Integer eloRating;
    private StatsDto stats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatsDto {
        private Integer gamesPlayed;
        private Integer wins;
        private Integer losses;
        private Integer winStreak;
        private Double avgGuesses;
        private Double avgSolveTimeSeconds;
    }
}

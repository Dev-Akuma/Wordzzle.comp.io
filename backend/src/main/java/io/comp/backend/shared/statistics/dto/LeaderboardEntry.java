package io.comp.backend.shared.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeaderboardEntry {
    private Integer rank;
    private String username;
    private Integer eloRating;
}

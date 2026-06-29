package io.comp.backend.shared.rating.service;

import org.springframework.stereotype.Service;

@Service
public class EloRatingService {
    private static final int K_FACTOR = 32;

    /**
     * Calculates Elo change for both players.
     * @param ratingA Rating of player A
     * @param ratingB Rating of player B
     * @param scoreA Actual outcome for player A: 1.0 (win), 0.5 (draw), 0.0 (loss)
     * @return an array of size 2 containing [changeA, changeB]
     */
    public int[] calculateElo(int ratingA, int ratingB, double scoreA) {
        double expectedA = 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
        double expectedB = 1.0 / (1.0 + Math.pow(10.0, (ratingA - ratingB) / 400.0));

        double scoreB = 1.0 - scoreA;

        int changeA = (int) Math.round(K_FACTOR * (scoreA - expectedA));
        int changeB = (int) Math.round(K_FACTOR * (scoreB - expectedB));

        return new int[]{changeA, changeB};
    }
}

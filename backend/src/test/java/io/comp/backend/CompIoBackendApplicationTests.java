package io.comp.backend;

import io.comp.backend.games.wordzzle.model.FeedbackType;
import io.comp.backend.games.wordzzle.service.WordzzleService;
import io.comp.backend.shared.rating.service.EloRatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CompIoBackendApplicationTests {

    @Autowired
    private WordzzleService wordzzleService;

    @Autowired
    private EloRatingService eloRatingService;

    @Test
    void contextLoads() {
    }

    @Test
    void testWordzzleFeedback() {
        // Target: APPLE, Guess: PUPPY
        // A P P L E (Target)
        // P U P P Y (Guess)
        // Expected: [PRESENT, ABSENT, CORRECT, ABSENT, ABSENT]
        List<FeedbackType> feedback = wordzzleService.checkWordleGuess("PUPPY", "APPLE");
        assertEquals(FeedbackType.PRESENT, feedback.get(0));
        assertEquals(FeedbackType.ABSENT, feedback.get(1));
        assertEquals(FeedbackType.CORRECT, feedback.get(2));
        assertEquals(FeedbackType.ABSENT, feedback.get(3));
        assertEquals(FeedbackType.ABSENT, feedback.get(4));

        // Exact match
        List<FeedbackType> exactFeedback = wordzzleService.checkWordleGuess("REACT", "REACT");
        for (FeedbackType f : exactFeedback) {
            assertEquals(FeedbackType.CORRECT, f);
        }
    }

    @Test
    void testEloRatingCalculation() {
        // Player A wins: scoreA = 1.0
        int[] changesWin = eloRatingService.calculateElo(1200, 1200, 1.0);
        assertEquals(16, changesWin[0]);
        assertEquals(-16, changesWin[1]);

        // Draw: scoreA = 0.5
        int[] changesDraw = eloRatingService.calculateElo(1200, 1200, 0.5);
        assertEquals(0, changesDraw[0]);
        assertEquals(0, changesDraw[1]);
    }
}

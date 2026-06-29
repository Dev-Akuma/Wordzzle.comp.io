package io.comp.backend.games.wordzzle.model;

import lombok.Data;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class WordzzleGameSession {
    private final UUID matchId;
    private final String hiddenWord;

    private final String player1Username;
    private final String player2Username;

    private int player1Attempts = 0;
    private int player2Attempts = 0;

    private final List<String> player1Guesses = new ArrayList<>();
    private final List<String> player2Guesses = new ArrayList<>();

    private final List<List<FeedbackType>> player1Feedback = new ArrayList<>();
    private final List<List<FeedbackType>> player2Feedback = new ArrayList<>();

    private boolean player1Solved = false;
    private boolean player2Solved = false;

    private Integer player1SolveTimeSeconds = null;
    private Integer player2SolveTimeSeconds = null;

    private boolean player1Finished = false;
    private boolean player2Finished = false;

    private Instant startTime;
    private boolean ended = false;
}

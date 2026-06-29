package io.comp.backend.games.wordzzle.service;

import io.comp.backend.games.wordzzle.model.Word;
import io.comp.backend.games.wordzzle.repository.WordRepository;
import io.comp.backend.shared.auth.repository.UserRepository;
import io.comp.backend.shared.statistics.repository.StatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Component
public class WordleDataInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(WordleDataInitializer.class);

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StatisticsRepository statisticsRepository;

    @Override
    public void run(String... args) throws Exception {
        if (wordRepository.count() == 0) {
            List<String> validWords = new java.util.ArrayList<>();
            try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("dictionary.txt")) {
                if (is != null) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim().toUpperCase();
                            if (line.length() == 5 && line.matches("[A-Z]{5}")) {
                                validWords.add(line);
                            }
                        }
                    }
                } else {
                    logger.error("Could not find dictionary.txt in resources!");
                }
            } catch (Exception e) {
                logger.error("Failed to load dictionary.txt", e);
            }

            if (!validWords.isEmpty()) {
                logger.info("Word list dictionary is empty. Seeding verified 5-letter words from dictionary.txt...");
                List<Word> wordsToSave = validWords.stream()
                        .distinct()
                        .map(w -> Word.builder().word(w).build())
                        .toList();
                wordRepository.saveAll(wordsToSave);
                logger.info("Wordle database seeding complete! Seeded {} words.", wordsToSave.size());
            } else {
                logger.warn("Seeded zero words because dictionary.txt was empty or missing.");
            }
        } else {
            logger.info("Wordle dictionary already seeded with {} words.", wordRepository.count());
        }

        seedBots();
    }

    private void seedBots() {
        List<String> botNames = Arrays.asList("VibeBot", "PuzzleBot", "LexiconBot", "WordWiz");
        List<Integer> botElos = Arrays.asList(1100, 1200, 1300, 1400);

        for (int i = 0; i < botNames.size(); i++) {
            String name = botNames.get(i);
            Integer elo = botElos.get(i);

            if (!userRepository.existsByUsername(name)) {
                logger.info("Seeding bot user: {} (Elo: {})", name, elo);
                io.comp.backend.shared.auth.model.User bot = io.comp.backend.shared.auth.model.User.builder()
                        .username(name)
                        .email(name.toLowerCase() + "@comp.io")
                        .passwordHash("")
                        .eloRating(elo)
                        .isBot(true)
                        .build();
                bot = userRepository.save(bot);

                statisticsRepository.save(io.comp.backend.shared.statistics.model.Statistics.builder()
                        .user(bot)
                        .build());
            }
        }
    }
}

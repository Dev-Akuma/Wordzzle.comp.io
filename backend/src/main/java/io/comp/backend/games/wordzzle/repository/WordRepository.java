package io.comp.backend.games.wordzzle.repository;

import io.comp.backend.games.wordzzle.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {
    Optional<Word> findByWord(String word);
    Boolean existsByWord(String word);

    @Query(value = "SELECT * FROM word_dictionary ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Word> findRandomWord();
}

package io.comp.backend.games.wordzzle.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "word_dictionary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 5)
    private String word;
}

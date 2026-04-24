package com.quizgen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(nullable = false, length = 500)
    private String option1;

    @Column(nullable = false, length = 500)
    private String option2;

    @Column(nullable = false, length = 500)
    private String option3;

    @Column(nullable = false, length = 500)
    private String option4;

    @Column(name = "correct_answer", nullable = false)
    private Integer correctAnswer; // 1, 2, 3, or 4
}

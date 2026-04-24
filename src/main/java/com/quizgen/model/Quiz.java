package com.quizgen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "quizzes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "study_material_id")
    private Long studyMaterialId;

    @Column(name = "generation_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private GenerationMethod generationMethod;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    public enum GenerationMethod {
        AI, RULE_BASED
    }
}

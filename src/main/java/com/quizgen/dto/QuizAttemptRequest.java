package com.quizgen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptRequest {
    private String studentName;
    private String studentUniqueId;
    private java.util.Map<Long, Integer> answers; // Map of question ID to selected answer (1-4)
}

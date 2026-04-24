package com.quizgen.dto;

import com.quizgen.model.Question;
import com.quizgen.model.Quiz;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {
    private Quiz quiz;
    private List<Question> questions;
}

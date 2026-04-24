package com.quizgen.controller;

import com.quizgen.dto.QuizAttemptRequest;
import com.quizgen.model.QuizAttempt;
import com.quizgen.service.QuizAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizAttemptController {

    private static final Logger logger = LoggerFactory.getLogger(QuizAttemptController.class);

    @Autowired
    private QuizAttemptService quizAttemptService;

    
    @PostMapping("/{quizId}/attempt")
    public ResponseEntity<?> submitAttempt(
            @PathVariable Long quizId,
            @RequestBody QuizAttemptRequest request) {

        try {
            if (request.getStudentName() == null || request.getStudentName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Student name is required"));
            }

            if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Answers are required"));
            }

            logger.info("Processing quiz attempt for student: {} on quiz: {}",
                    request.getStudentName(), quizId);

            QuizAttempt attempt = quizAttemptService.submitQuizAttempt(
                    quizId,
                    request.getStudentName(),
                    request.getStudentUniqueId(),
                    request.getAnswers());

            return ResponseEntity.ok(attempt);

        } catch (Exception e) {
            logger.error("Error submitting quiz attempt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to submit quiz attempt: " + e.getMessage()));
        }
    }

    
    @GetMapping("/{quizId}/attempts")
    public ResponseEntity<List<QuizAttempt>> getQuizAttempts(@PathVariable Long quizId) {
        List<QuizAttempt> attempts = quizAttemptService.getAttemptsByQuiz(quizId);
        return ResponseEntity.ok(attempts);
    }

    
    @GetMapping("/attempts/student/{studentName}")
    public ResponseEntity<List<QuizAttempt>> getStudentAttempts(@PathVariable String studentName) {
        List<QuizAttempt> attempts = quizAttemptService.getAttemptsByStudent(studentName);
        return ResponseEntity.ok(attempts);
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}

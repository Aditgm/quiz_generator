package com.quizgen.controller;

import com.quizgen.model.QuizAttempt;
import com.quizgen.service.LeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    private static final Logger logger = LoggerFactory.getLogger(LeaderboardController.class);

    @Autowired
    private LeaderboardService leaderboardService;

    
    @GetMapping
    public ResponseEntity<List<QuizAttempt>> getGlobalLeaderboard(
            @RequestParam(required = false, defaultValue = "50") int limit) {

        logger.info("Fetching global leaderboard (limit: {})", limit);
        List<QuizAttempt> leaderboard = leaderboardService.getGlobalLeaderboard(limit);
        return ResponseEntity.ok(leaderboard);
    }

    
    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<List<QuizAttempt>> getQuizLeaderboard(
            @PathVariable Long quizId,
            @RequestParam(required = false, defaultValue = "50") int limit) {

        logger.info("Fetching leaderboard for quiz {} (limit: {})", quizId, limit);
        List<QuizAttempt> leaderboard = leaderboardService.getQuizLeaderboard(quizId, limit);
        return ResponseEntity.ok(leaderboard);
    }
}

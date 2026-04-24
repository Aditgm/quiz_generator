package com.quizgen.service;

import com.quizgen.model.QuizAttempt;
import com.quizgen.repository.QuizAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardService {

    private static final Logger logger = LoggerFactory.getLogger(LeaderboardService.class);

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    
    public List<QuizAttempt> getGlobalLeaderboard() {
        return getGlobalLeaderboard(50); // Default top 50
    }

    
    public List<QuizAttempt> getGlobalLeaderboard(int limit) {
        logger.info("Fetching global leaderboard (top {})", limit);
        List<QuizAttempt> topScorers = quizAttemptRepository.findTopScorers();

        if (topScorers.size() > limit) {
            return topScorers.subList(0, limit);
        }

        return topScorers;
    }

    
    public List<QuizAttempt> getQuizLeaderboard(Long quizId) {
        return getQuizLeaderboard(quizId, 50); // Default top 50
    }

    
    public List<QuizAttempt> getQuizLeaderboard(Long quizId, int limit) {
        logger.info("Fetching leaderboard for quiz {} (top {})", quizId, limit);
        List<QuizAttempt> topScorers = quizAttemptRepository.findTopScorersByQuiz(quizId);

        if (topScorers.size() > limit) {
            return topScorers.subList(0, limit);
        }

        return topScorers;
    }
}

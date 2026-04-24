package com.quizgen.repository;

import com.quizgen.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByQuizId(Long quizId);

    List<QuizAttempt> findByStudentName(String studentName);

    @Query("SELECT qa FROM QuizAttempt qa ORDER BY qa.score DESC, qa.attemptDate ASC")
    List<QuizAttempt> findTopScorers();

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.quizId = :quizId ORDER BY qa.score DESC, qa.attemptDate ASC")
    List<QuizAttempt> findTopScorersByQuiz(Long quizId);
}

package com.aptigen.attempt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {
    List<AttemptAnswer> findByAttemptId(Long attemptId);

    @Query("""
           SELECT q.topic AS topic,
                  SUM(CASE WHEN a.isCorrect = true THEN 1 ELSE 0 END) AS correctCount,
                  COUNT(a.id) AS totalCount,
                  MAX(ta.submittedAt) AS lastAttemptAt
           FROM AttemptAnswer a
           JOIN TestAttempt ta ON ta.id = a.attemptId
           JOIN com.aptigen.generation.McqQuestion q ON q.id = a.questionId
           WHERE ta.userId = :userId
           GROUP BY q.topic
           """)
    List<TopicAccuracyRow> getTopicAccuracy(Long userId);

    interface TopicAccuracyRow {
        String getTopic();
        Long getCorrectCount();
        Long getTotalCount();
        java.time.LocalDateTime getLastAttemptAt();
    }

    @Query("""
       SELECT q.topic AS topic,
              SUM(CASE WHEN a.isCorrect = true THEN 1 ELSE 0 END) AS correctCount,
              COUNT(a.id) AS totalCount,
                  MAX(ta.submittedAt) AS lastAttemptAt
           FROM AttemptAnswer a
       JOIN TestAttempt ta ON ta.id = a.attemptId
       JOIN com.aptigen.generation.McqQuestion q ON q.id = a.questionId
       GROUP BY q.topic
       """)
    List<TopicAccuracyRow> getPlatformTopicAccuracy();
}
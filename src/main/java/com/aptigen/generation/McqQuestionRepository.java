package com.aptigen.generation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface McqQuestionRepository extends JpaRepository<McqQuestion, Long> {
    List<McqQuestion> findByTestId(Long testId);
    long countByTestId(Long testId);
}
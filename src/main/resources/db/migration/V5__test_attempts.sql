   CREATE TABLE test_attempts (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       test_id BIGINT NOT NULL,
       user_id BIGINT NOT NULL,
       started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       submitted_at TIMESTAMP NULL,
       score INT NULL,
       total_questions INT NOT NULL,
       coins_spent INT NOT NULL DEFAULT 0,
       cashback_awarded BOOLEAN NOT NULL DEFAULT FALSE,
       CONSTRAINT fk_attempt_test FOREIGN KEY (test_id) REFERENCES mcq_tests(id),
       CONSTRAINT fk_attempt_user FOREIGN KEY (user_id) REFERENCES users(id)
   );

   CREATE TABLE attempt_answers (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       attempt_id BIGINT NOT NULL,
       question_id BIGINT NOT NULL,
       selected_option CHAR(1) NULL,
       is_correct BOOLEAN NULL,
       CONSTRAINT fk_answer_attempt FOREIGN KEY (attempt_id) REFERENCES test_attempts(id),
       CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES mcq_questions(id)
   );
   CREATE TABLE weekly_contests (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       test_id BIGINT NOT NULL,
       scheduled_date DATE NOT NULL,
       entry_fee_coins INT NOT NULL DEFAULT 5,
       status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_contest_test FOREIGN KEY (test_id) REFERENCES mcq_tests(id)
   );

   CREATE TABLE contest_entries (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       contest_id BIGINT NOT NULL,
       user_id BIGINT NOT NULL,
       attempt_id BIGINT NULL,
       contest_rank INT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_entry_contest FOREIGN KEY (contest_id) REFERENCES weekly_contests(id),
       CONSTRAINT fk_entry_user FOREIGN KEY (user_id) REFERENCES users(id),
       CONSTRAINT fk_entry_attempt FOREIGN KEY (attempt_id) REFERENCES test_attempts(id),
       UNIQUE KEY unique_user_per_contest (contest_id, user_id)
   );
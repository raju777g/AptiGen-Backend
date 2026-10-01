   CREATE TABLE mcq_tests (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       owner_user_id BIGINT NOT NULL,
       title VARCHAR(255) NOT NULL,
       is_public BOOLEAN NOT NULL DEFAULT FALSE,
       timer_mode VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
       seconds_per_question INT NOT NULL DEFAULT 120,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_test_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
   );

   CREATE TABLE mcq_questions (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       test_id BIGINT NOT NULL,
       question_text TEXT NOT NULL,
       option_a VARCHAR(500) NOT NULL,
       option_b VARCHAR(500) NOT NULL,
       option_c VARCHAR(500) NOT NULL,
       option_d VARCHAR(500) NOT NULL,
       correct_option CHAR(1) NOT NULL,
       topic VARCHAR(100),
       CONSTRAINT fk_question_test FOREIGN KEY (test_id) REFERENCES mcq_tests(id)
   );
   CREATE TABLE password_reset_otps (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       user_id BIGINT NOT NULL,
       otp_hash VARCHAR(255) NOT NULL,
       expires_at TIMESTAMP NOT NULL,
       used BOOLEAN NOT NULL DEFAULT FALSE,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_reset_otp_user FOREIGN KEY (user_id) REFERENCES users(id)
   );
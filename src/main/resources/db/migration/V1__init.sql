   CREATE TABLE users (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       email VARCHAR(255) NOT NULL UNIQUE,
       password_hash VARCHAR(255) NULL,
       auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
       provider_id VARCHAR(255) NULL,
       email_verified BOOLEAN NOT NULL DEFAULT FALSE,
       verification_token VARCHAR(255) NULL,
       referral_code VARCHAR(20) NOT NULL UNIQUE,
       referred_by_user_id BIGINT NULL,
       current_streak INT NOT NULL DEFAULT 0,
       last_login_date DATE NULL,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_referred_by FOREIGN KEY (referred_by_user_id) REFERENCES users(id)
   );

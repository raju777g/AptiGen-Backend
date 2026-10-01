   CREATE TABLE referrals (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       referrer_user_id BIGINT NOT NULL,
       referee_user_id BIGINT NOT NULL UNIQUE,
       rewarded BOOLEAN NOT NULL DEFAULT FALSE,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_referral_referrer FOREIGN KEY (referrer_user_id) REFERENCES users(id),
       CONSTRAINT fk_referral_referee FOREIGN KEY (referee_user_id) REFERENCES users(id)
   );
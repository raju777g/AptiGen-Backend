   CREATE TABLE notifications (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       user_id BIGINT NULL,  -- NULL = platform-wide "From Us" announcement
       title VARCHAR(255) NOT NULL,
       message VARCHAR(1000) NOT NULL,
       is_read BOOLEAN NOT NULL DEFAULT FALSE,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id)
   );
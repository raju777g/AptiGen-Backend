ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN blocked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN block_message VARCHAR(500) NULL;

CREATE TABLE user_activity_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_activity_user_time (user_id, occurred_at)
);

   CREATE TABLE support_chats (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       user_id BIGINT NOT NULL,
       subject VARCHAR(255) NOT NULL,
       status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_chat_user FOREIGN KEY (user_id) REFERENCES users(id)
   );

   CREATE TABLE support_messages (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       chat_id BIGINT NOT NULL,
       sender_type VARCHAR(10) NOT NULL,
       message TEXT NULL,
       attachment_url VARCHAR(255) NULL,
       attachment_type VARCHAR(20) NULL,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_message_chat FOREIGN KEY (chat_id) REFERENCES support_chats(id)
   );
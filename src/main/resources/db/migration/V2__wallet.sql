   CREATE TABLE coin_wallets (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       user_id BIGINT NOT NULL UNIQUE,
       balance INT NOT NULL DEFAULT 0,
       CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id)
   );

   CREATE TABLE coin_transactions (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       user_id BIGINT NOT NULL,
       amount INT NOT NULL,
       type VARCHAR(30) NOT NULL,
       reference_id BIGINT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_txn_user FOREIGN KEY (user_id) REFERENCES users(id)
   );
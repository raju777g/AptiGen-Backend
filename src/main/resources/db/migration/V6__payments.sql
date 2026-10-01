   CREATE TABLE payments (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       user_id BIGINT NOT NULL,
       razorpay_order_id VARCHAR(100) NOT NULL UNIQUE,
       razorpay_payment_id VARCHAR(100) NULL,
       amount_inr INT NOT NULL,
       coins_credited INT NOT NULL,
       status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id)
   );
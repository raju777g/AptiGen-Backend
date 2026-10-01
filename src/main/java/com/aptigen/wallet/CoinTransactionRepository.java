package com.aptigen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {
    List<CoinTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CoinTransaction> findByCreatedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);
}

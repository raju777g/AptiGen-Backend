package com.aptigen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CoinWalletRepository extends JpaRepository<CoinWallet, Long> {
    Optional<CoinWallet> findByUserId(Long userId);
}
package com.aptigen.wallet;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final CoinWalletRepository walletRepository;
    private final CoinTransactionRepository transactionRepository;

    @Transactional
    public CoinWallet createWallet(Long userId) {
        CoinWallet wallet = CoinWallet.builder()
                .userId(userId)
                .balance(0)
                .build();
        return walletRepository.save(wallet);
    }

    public CoinWallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No wallet for user " + userId));
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void credit(Long userId, int amount, TransactionType type, Long referenceId) {
        CoinWallet wallet = getWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        transactionRepository.save(CoinTransaction.builder()
                .userId(userId)
                .amount(amount)
                .type(type)
                .referenceId(referenceId)
                .build());
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void debit(Long userId, int amount, TransactionType type, Long referenceId) {
        CoinWallet wallet = getWallet(userId);
        if (wallet.getBalance() < amount) {
            throw new InsufficientCoinsException("Not enough AG coins. Balance: " + wallet.getBalance());
        }
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        transactionRepository.save(CoinTransaction.builder()
                .userId(userId)
                .amount(-amount)
                .type(type)
                .referenceId(referenceId)
                .build());
    }
}
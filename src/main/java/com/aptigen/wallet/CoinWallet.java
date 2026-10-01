package com.aptigen.wallet;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coin_wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    @Builder.Default
    private int balance = 0;

    @Version // optimistic locking — protects against two concurrent debits both reading the same stale balance
    private Long version;
}
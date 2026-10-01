package com.aptigen.wallet;

import com.aptigen.common.AuthUtil;
import com.aptigen.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;
    private  final AuthUtil authUtil;
    private final CoinTransactionRepository transactionRepository;

    @GetMapping
    public ResponseEntity<?> getWallet(Authentication auth) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        CoinWallet wallet = walletService.getWallet(userId);
        return ResponseEntity.ok(new WalletResponse(wallet.getBalance(), transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)));
    }

    record WalletResponse(int balance, java.util.List<CoinTransaction> transactions) {}
}

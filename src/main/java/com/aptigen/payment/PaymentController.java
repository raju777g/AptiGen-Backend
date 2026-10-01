package com.aptigen.payment;

import com.aptigen.common.AuthUtil;
import com.aptigen.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;
    private final AuthUtil authUtil;

    record CreateOrderRequest(int amountInr) {}

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(Authentication auth, @RequestBody CreateOrderRequest request) throws Exception {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        Map<String, Object> result = paymentService.createOrder(userId, request.amountInr());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature
    ) throws Exception {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
package com.aptigen.payment;

import com.razorpay.Utils;
import com.aptigen.wallet.TransactionType;
import com.aptigen.wallet.WalletService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;


    private final com.aptigen.wallet.WalletService walletService;

    private static final int COINS_PER_RUPEE = 10; // ₹10 → 100 coins ratio from your original spec
    private static final int MIN_AMOUNT_INR = 1;
    private static final int MAX_AMOUNT_INR = 5000;

    private final com.aptigen.user.UserService userService;
    private final com.aptigen.email.EmailService emailService;
    private final com.aptigen.notification.NotificationService notificationService;

    public Map<String, Object> createOrder(Long userId, int amountInr) throws Exception {
        if (amountInr < MIN_AMOUNT_INR || amountInr > MAX_AMOUNT_INR) {
            throw new IllegalArgumentException(
                    "Amount must be between ₹" + MIN_AMOUNT_INR + " and ₹" + MAX_AMOUNT_INR);
        }

        int coinsToCredit = amountInr * COINS_PER_RUPEE;

        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInr * 100); // paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "aptigen_" + userId + "_" + System.currentTimeMillis());

        Order order = client.orders.create(orderRequest);
        String orderId = (String) order.get("id");

        paymentRepository.save(Payment.builder()
                .userId(userId)
                .razorpayOrderId(orderId)
                .amountInr(amountInr)
                .coinsCredited(coinsToCredit)
                .build());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderId", orderId);
        response.put("amount", amountInr * 100);
        response.put("currency", "INR");
        response.put("keyId", keyId);
        response.put("coinsToCredit", coinsToCredit);
        return response;
    }

    public void handleWebhook(String payload, String signature) throws Exception {
        boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        if (!isValid) {
            throw new SecurityException("Invalid webhook signature");
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");

        if (!"payment.captured".equals(eventType)) {
            return; // ignore other event types for now
        }

        JSONObject paymentEntity = event.getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        String orderId = paymentEntity.getString("order_id");
        String paymentId = paymentEntity.getString("id");

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("Unknown order: " + orderId));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return; // idempotent — webhook retries shouldn't double-credit
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setRazorpayPaymentId(paymentId);
        paymentRepository.save(payment);

        walletService.credit(payment.getUserId(), payment.getCoinsCredited(), TransactionType.PURCHASE, payment.getId());

        String userEmail = userService.findById(payment.getUserId()).getEmail();
        emailService.sendPurchaseConfirmationEmail(userEmail, payment.getAmountInr(), payment.getCoinsCredited());

        notificationService.notifyUser(payment.getUserId(), "Coins Credited",
                payment.getCoinsCredited() + " AG coins added to your wallet (₹" + payment.getAmountInr() + ")");
    }
}
package com.aptigen.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.from-email}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public boolean sendVerificationOtpEmail(String to, String otp) {
        String content = """
                <p style="margin:0 0 18px;color:#475569;font-size:16px;line-height:1.7">Enter this one-time code in AptiGen to verify your email and activate your account.</p>
                <div style="margin:24px 0;padding:20px 12px;border:1px solid #e9e5f5;border-radius:12px;background:#f7f5ff;text-align:center;color:#5b21b6;font-size:34px;font-weight:700;letter-spacing:12px">%s</div>
                <p style="margin:0 0 18px;color:#64748b;font-size:13px;line-height:1.7">This code expires in 10 minutes. After verification, we'll add your 50 AG coin welcome bonus.</p>
                <p style="margin:0;color:#94a3b8;font-size:12px;line-height:1.6">If you didn't create an AptiGen account, you can ignore this email.</p>
                """.formatted(escapeHtml(otp));
        return send(to, "Your AptiGen verification code", emailLayout("Your AptiGen email verification code expires in 10 minutes.", "Verify your email", content));
    }

    public void sendWelcomeEmail(String to) {
        String content = """
                <p style="margin:0 0 18px;color:#475569;font-size:16px;line-height:1.7">Your account is ready, and <strong style="color:#6d28d9">50 AG coins</strong> have been added to your wallet. Here's what you can do next:</p>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse">
                  <tr><td style="padding:12px 14px;border:1px solid #e9e5f5;border-radius:10px;color:#334155;font-size:14px;line-height:1.6"><strong style="color:#6d28d9">Create practice tests</strong><br>Turn MCQ images into timed tests.</td></tr>
                  <tr><td height="10" style="font-size:0;line-height:0">&nbsp;</td></tr>
                  <tr><td style="padding:12px 14px;border:1px solid #e9e5f5;border-radius:10px;color:#334155;font-size:14px;line-height:1.6"><strong style="color:#6d28d9">Practice and improve</strong><br>Try public tests and track your progress by topic.</td></tr>
                  <tr><td height="10" style="font-size:0;line-height:0">&nbsp;</td></tr>
                  <tr><td style="padding:12px 14px;border:1px solid #e9e5f5;border-radius:10px;color:#334155;font-size:14px;line-height:1.6"><strong style="color:#6d28d9">Publish your tests</strong><br>Share your questions and earn creator royalties.</td></tr>
                </table>
                <p style="margin:28px 0;text-align:center"><a href="%s/dashboard" style="display:inline-block;padding:14px 28px;border-radius:10px;background:#6d28d9;color:#ffffff;font-size:15px;font-weight:700;text-decoration:none">Open AptiGen</a></p>
                """.formatted(trimTrailingSlash(frontendUrl));
        send(to, "Welcome to AptiGen", emailLayout("Your account is ready. Your 50 AG coin bonus is waiting.", "Welcome to AptiGen!", content));
    }

    public void sendPurchaseConfirmationEmail(String to, int amountInr, int coinsCredited) {
        String content = """
                <p style="margin:0 0 20px;color:#475569;font-size:16px;line-height:1.7">Your payment went through successfully. Your coins are ready to use.</p>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;background:#f7f5ff;border-radius:12px">
                  <tr><td style="padding:18px 20px;color:#64748b;font-size:14px">Coins added</td><td align="right" style="padding:18px 20px;color:#6d28d9;font-size:20px;font-weight:700">%d AG</td></tr>
                  <tr><td colspan="2" style="padding:0 20px"><div style="height:1px;background:#e6e0f4"></div></td></tr>
                  <tr><td style="padding:18px 20px;color:#64748b;font-size:14px">Amount paid</td><td align="right" style="padding:18px 20px;color:#172033;font-size:16px;font-weight:700">&#8377;%d</td></tr>
                </table>
                <p style="margin:24px 0 0;color:#475569;font-size:15px;line-height:1.7">Thanks for supporting AptiGen. Happy practicing!</p>
                """.formatted(coinsCredited, amountInr);
        send(to, "Your AptiGen coins are ready", emailLayout("Payment successful. Your AG coins have been credited.", "Payment successful", content));
    }

    public boolean sendPasswordResetOtp(String to, String otp) {
        String content = """
                <p style="margin:0 0 18px;color:#475569;font-size:16px;line-height:1.7">Use this one-time code to reset your AptiGen password. It expires in 10 minutes.</p>
                <div style="margin:24px 0;padding:20px 12px;border:1px solid #e9e5f5;border-radius:12px;background:#f7f5ff;text-align:center;color:#5b21b6;font-size:32px;font-weight:700;letter-spacing:10px">%s</div>
                <p style="margin:0;color:#64748b;font-size:13px;line-height:1.7">If you didn't request a password reset, you can safely ignore this email. Your password won't change unless this code is used.</p>
                """.formatted(escapeHtml(otp));
        return send(to, "Your AptiGen password reset code", emailLayout("Your one-time password reset code expires in 10 minutes.", "Reset your password", content));
    }

    private String emailLayout(String preheader, String title, String content) {
        String logoUrl = trimTrailingSlash(frontendUrl) + "/logo-mark.png";
        return """
                <!doctype html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light"><title>%s</title></head>
                <body style="margin:0;padding:0;background:#f1eff8;font-family:Arial,Helvetica,sans-serif;color:#172033">
                  <div style="display:none!important;visibility:hidden;opacity:0;color:transparent;height:0;width:0;overflow:hidden">%s</div>
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f1eff8;border-collapse:collapse">
                    <tr><td align="center" style="padding:36px 16px">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px;border-collapse:separate;border-spacing:0;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 12px 36px rgba(35,20,70,.10)">
                        <tr><td align="center" style="padding:28px 24px;background:#17132b;background-image:linear-gradient(135deg,#17132b,#30205a)">
                          <img src="%s" width="44" height="44" alt="AptiGen logo" style="display:inline-block;vertical-align:middle;width:44px;height:44px;border:0;border-radius:12px">
                          <span style="display:inline-block;vertical-align:middle;margin-left:10px;color:#ffffff;font-size:25px;font-weight:700;letter-spacing:-.5px">AptiGen</span>
                          <div style="margin-top:9px;color:#c4b5fd;font-size:12px;letter-spacing:1.4px;text-transform:uppercase">Learn · Practice · Improve</div>
                        </td></tr>
                        <tr><td style="padding:34px 38px 38px">
                          <h1 style="margin:0 0 22px;color:#172033;font-size:25px;line-height:1.3;text-align:center">%s</h1>
                          %s
                        </td></tr>
                        <tr><td style="padding:20px 28px;border-top:1px solid #eeeaf5;text-align:center;color:#94a3b8;font-size:12px;line-height:1.7">
                          <div style="color:#64748b;font-weight:700">AptiGen</div>
                          <div>Smarter practice, one question at a time.</div>
                          <div style="margin-top:6px">Need help? <a href="mailto:rajugarain64@gmail.com" style="color:#6d28d9;text-decoration:none">Contact support</a></div>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escapeHtml(title), escapeHtml(preheader), escapeHtml(logoUrl), escapeHtml(title), content);
    }

    private boolean send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

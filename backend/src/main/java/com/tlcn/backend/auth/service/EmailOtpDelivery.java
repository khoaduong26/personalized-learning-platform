package com.tlcn.backend.auth.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailOtpDelivery implements OtpDelivery {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    public EmailOtpDelivery(ObjectProvider<JavaMailSender> mailSender,
                            @Value("${app.auth.mail-from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }
    @Override
    public void send(String email, String otp) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from.isBlank()) throw unavailable();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("TLCN – Xác minh tài khoản");
        message.setText("Mã xác minh của bạn là: " + otp
                + "\nMã có hiệu lực trong 5 phút. Không chia sẻ mã này với người khác."
                + "\nNếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.");
        try { sender.send(message); }
        catch (MailException ex) { throw unavailable(); }
    }
    private AuthException unavailable() {
        return new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_UNAVAILABLE",
                "Chưa gửi được email xác minh. Vui lòng thử lại sau.");
    }
}

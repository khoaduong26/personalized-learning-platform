package com.tlcn.backend.auth.service.serviceImpl;

import com.tlcn.backend.auth.service.OtpDelivery;
import com.tlcn.backend.auth.exception.AuthException;

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
        sendMail(email, "TLCN – Xác minh tài khoản", "Mã xác minh của bạn là: " + otp
                + "\nMã có hiệu lực trong 5 phút. Không chia sẻ mã này với người khác."
                + "\nNếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.");
    }

    @Override
    public void sendPasswordResetCode(String email, String otp) {
        sendMail(email, "TLCN – Mã đặt lại mật khẩu", "Mã đặt lại mật khẩu của bạn là: " + otp
                + "\nMã có hiệu lực trong 5 phút. Không chia sẻ mã này với người khác."
                + "\nNếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.");
    }

    @Override
    public void sendPasswordResetNotice(String email) {
        sendMail(email, "TLCN – Mật khẩu đã được đặt lại", "Mật khẩu tài khoản EduLearn của bạn đã được đặt lại."
                + "\nNếu đây không phải là bạn, hãy đặt lại mật khẩu và liên hệ quản trị viên.");
    }

    private void sendMail(String email, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from.isBlank())
            throw unavailable();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        try {
            sender.send(message);
        } catch (MailException ex) {
            throw unavailable();
        }
    }

    private AuthException unavailable() {
        return new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_UNAVAILABLE",
                "Chưa gửi được email xác minh. Vui lòng thử lại sau.");
    }
}

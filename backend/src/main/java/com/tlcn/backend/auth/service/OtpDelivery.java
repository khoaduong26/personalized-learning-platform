package com.tlcn.backend.auth.service;

public interface OtpDelivery {
    void send(String email, String otp);
    void sendPasswordResetCode(String email, String otp);
    void sendPasswordResetNotice(String email);
}

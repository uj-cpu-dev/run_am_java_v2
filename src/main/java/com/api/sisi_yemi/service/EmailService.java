package com.api.sisi_yemi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    @Value("${spring.mail.from}")
    private String from;

    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Verify your email";
        String verificationUrl = "https://runam.africa/verify-user?token=" + token;

        String body = "Hi,\n\nPlease click the link below to verify your email. This link will expire in 3 hours:\n\n"
                + verificationUrl + "\n\nThanks!";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(from);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}

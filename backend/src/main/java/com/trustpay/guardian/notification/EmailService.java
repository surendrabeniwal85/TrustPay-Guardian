package com.trustpay.guardian.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service dispatching simulated email alerts (e.g. through MailHog) for security critical events.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final Optional<JavaMailSender> mailSender;

    public EmailService(Optional<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        if (mailSender.isPresent()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("alerts@trustpay.internal");
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.get().send(message);
                log.info("Email alert dispatched to [{}] with subject [{}]", to, subject);
            } catch (Exception e) {
                log.warn("Failed to dispatch email to [{}]: {}", to, e.getMessage());
            }
        } else {
            log.debug("JavaMailSender not available; skipping email delivery to [{}]", to);
        }
    }
}

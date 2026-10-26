package com.trustpay.guardian.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    @DisplayName("Email sending dispatches message when JavaMailSender is present")
    void sendEmailSuccess() {
        EmailService emailService = new EmailService(Optional.of(mailSender));

        assertThatCode(() -> emailService.sendEmail("user@example.com", "Test Subject", "Test Body"))
                .doesNotThrowAnyException();

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Email sending skips gracefully when JavaMailSender is absent")
    void sendEmailSkippedWhenSenderAbsent() {
        EmailService emailService = new EmailService(Optional.empty());

        assertThatCode(() -> emailService.sendEmail("user@example.com", "Test Subject", "Test Body"))
                .doesNotThrowAnyException();

        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Email sending catches and logs MailSendException without blowing up caller")
    void sendEmailHandlesExceptionGracefully() {
        doThrow(new MailSendException("SMTP connection refused")).when(mailSender).send(any(SimpleMailMessage.class));
        EmailService emailService = new EmailService(Optional.of(mailSender));

        assertThatCode(() -> emailService.sendEmail("user@example.com", "Test Subject", "Test Body"))
                .doesNotThrowAnyException();

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}

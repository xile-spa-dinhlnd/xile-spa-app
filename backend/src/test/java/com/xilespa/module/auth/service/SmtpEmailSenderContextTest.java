package com.xilespa.module.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.xilespa.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(
        properties = {
            "xile.mail.sender=smtp",
            "spring.mail.host=localhost",
            "spring.mail.port=2525",
            "xile.mail.from=test@xilespa.vn"
        })
class SmtpEmailSenderContextTest {

    @Autowired private ApplicationContext applicationContext;

    @Autowired private PasswordResetEmailSender emailSender;

    @Test
    @DisplayName(
            "Khi cấu hình xile.mail.sender=smtp, bean SmtpPasswordResetEmailSender được kích hoạt")
    void whenSenderIsSmtp_smtpBeanIsCreated() {
        assertThat(emailSender).isInstanceOf(SmtpPasswordResetEmailSender.class);
        assertThat(applicationContext.containsBean("smtpPasswordResetEmailSender")).isTrue();
    }
}

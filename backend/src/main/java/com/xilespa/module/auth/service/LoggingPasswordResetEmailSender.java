package com.xilespa.module.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Triển khai gửi email môi trường phát triển và kiểm thử (OQ-10, FR-AUTH-03). Không gửi email thật
 * mà ghi đường link đặt lại mật khẩu ra log để nhà phát triển kiểm thử. Được kích hoạt khi cấu hình
 * {@code xile.mail.sender=log} (hoặc mặc định).
 */
@Component
@ConditionalOnProperty(name = "xile.mail.sender", havingValue = "log", matchIfMissing = true)
public class LoggingPasswordResetEmailSender implements PasswordResetEmailSender {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingPasswordResetEmailSender.class);

    @Override
    @Async
    public void sendResetEmail(String toEmail, String resetLink) {
        log.info("==================== [DEV EMAIL SENDER (OQ-10)] ====================");
        log.info("Gửi email đặt lại mật khẩu tới: {}", toEmail);
        log.info("Liên kết đặt lại mật khẩu: {}", resetLink);
        log.info("====================================================================");
    }
}

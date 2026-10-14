package com.xilespa.module.auth.service;

import com.xilespa.module.auth.config.AuthSecurityProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Triển khai gửi email đặt lại mật khẩu qua SMTP trong môi trường Production (OQ-10, FR-AUTH-03).
 * Được kích hoạt khi cấu hình {@code xile.mail.sender=smtp}. Tuyệt đối không ghi liên kết hoặc
 * token nhạy cảm ra log (bảo mật tuyệt đối theo NFR-SEC-01).
 */
@Component
@ConditionalOnProperty(name = "xile.mail.sender", havingValue = "smtp")
public class SmtpPasswordResetEmailSender implements PasswordResetEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpPasswordResetEmailSender.class);

    private final JavaMailSender mailSender;
    private final AuthSecurityProperties authSecurityProperties;
    private final String fromEmail;

    public SmtpPasswordResetEmailSender(
            JavaMailSender mailSender,
            AuthSecurityProperties authSecurityProperties,
            @Value("${xile.mail.from}") String fromEmail) {
        this.mailSender = mailSender;
        this.authSecurityProperties = authSecurityProperties;
        this.fromEmail = fromEmail;
    }

    @Override
    @Async
    public void sendResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Xile Spa & Giãn cơ - Yêu cầu đặt lại mật khẩu");

            String durationText =
                    authSecurityProperties.passwordResetDuration().toMinutes() + " phút";
            String htmlBody =
                    """
                    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 24px; border: 1px solid #e8e8e8; border-radius: 8px;">
                      <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #2b5a3e; margin: 0 0 8px;">Xile Spa & Giãn cơ</h2>
                        <p style="color: #666; font-size: 14px; margin: 0;">Hệ thống quản trị tiệm</p>
                      </div>
                      <p style="font-size: 15px; color: #333; line-height: 1.6;">Xin chào,</p>
                      <p style="font-size: 15px; color: #333; line-height: 1.6;">Hệ thống nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Vui lòng bấm vào nút bên dưới để tiến hành tạo mật khẩu mới:</p>
                      <div style="text-align: center; margin: 32px 0;">
                        <a href="%s" style="background-color: #2b5a3e; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 15px; display: inline-block;">Đặt lại mật khẩu</a>
                      </div>
                      <p style="font-size: 13px; color: #888; line-height: 1.5;">Liên kết này có hiệu lực trong vòng <strong>%s</strong> và chỉ sử dụng được một lần duy nhất.</p>
                      <p style="font-size: 13px; color: #888; line-height: 1.5;">Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này. Tài khoản của bạn vẫn được bảo vệ an toàn.</p>
                      <hr style="border: none; border-top: 1px solid #f0f0f0; margin: 24px 0;" />
                      <p style="font-size: 12px; color: #aaa; text-align: center; margin: 0;">Xile Beauty & Spa - Thư thông báo tự động, vui lòng không trả lời thư này.</p>
                    </div>
                    """
                            .formatted(resetLink, durationText);

            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Đã gửi email đặt lại mật khẩu thành công tới tài khoản.");
        } catch (MessagingException | MailException e) {
            log.error("Không thể gửi email đặt lại mật khẩu qua máy chủ SMTP", e);
        }
    }
}

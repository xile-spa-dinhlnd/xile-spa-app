package com.xilespa.module.auth.service;

import com.xilespa.module.auth.config.OwnerInitializerProperties;
import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.entity.UserRole;
import com.xilespa.module.auth.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tự động khởi tạo tài khoản chủ tiệm ở lần chạy đầu tiên từ biến môi trường (FR-AUTH-07). - Không
 * có trang đăng ký công khai. - Idempotent: chạy lại nhiều lần không tạo trùng hoặc ghi đè. - Tuyệt
 * đối không ghi mật khẩu ra log (NFR-SEC-02).
 */
@Component
public class OwnerAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnerAccountInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OwnerInitializerProperties properties;

    public OwnerAccountInitializer(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OwnerInitializerProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.info(
                    "Không có cấu hình tài khoản chủ tiệm (xile.init.owner), bỏ qua bước khởi tạo.");
            return;
        }

        String email = properties.email().trim().toLowerCase();
        if (userRepository.existsByRole(UserRole.OWNER)
                || userRepository.existsByEmailIgnoreCase(email)) {
            log.info("Tài khoản chủ tiệm hoặc email '{}' đã tồn tại, bỏ qua bước khởi tạo.", email);
            return;
        }

        String displayName =
                (properties.displayName() != null && !properties.displayName().isBlank())
                        ? properties.displayName().trim()
                        : "Chủ tiệm Xile";

        AppUser owner =
                new AppUser(
                        email,
                        passwordEncoder.encode(properties.password()),
                        displayName,
                        UserRole.OWNER);

        userRepository.save(owner);
        log.info(
                "Khởi tạo tài khoản chủ tiệm thành công: email='{}', displayName='{}'",
                email,
                displayName);
    }
}

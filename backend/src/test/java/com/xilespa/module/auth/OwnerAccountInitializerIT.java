package com.xilespa.module.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.entity.UserRole;
import com.xilespa.module.auth.repository.AppUserRepository;
import com.xilespa.support.IntegrationTest;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(
        properties = {
            "xile.init.owner.enabled=true",
            "xile.init.owner.email=it_owner@xilespa.vn",
            "xile.init.owner.password=it_password_123",
            "xile.init.owner.display-name=Chủ tiệm IT"
        })
class OwnerAccountInitializerIT {

    @Autowired private AppUserRepository userRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        // Dọn dẹp để không để lại dữ liệu trong container dùng chung giữa các test
        userRepository.deleteAll();
    }

    @Test
    @DisplayName(
            "Khởi tạo và lưu tài khoản chủ tiệm vào PostgreSQL thật với mật khẩu được băm BCrypt")
    void shouldInitializeOwnerInDatabase() {
        // Arrange & Act
        Optional<AppUser> ownerOpt = userRepository.findByEmailIgnoreCase("it_owner@xilespa.vn");

        // Assert
        assertThat(ownerOpt).isPresent();
        AppUser owner = ownerOpt.get();
        assertThat(owner.getEmail()).isEqualTo("it_owner@xilespa.vn");
        assertThat(owner.getDisplayName()).isEqualTo("Chủ tiệm IT");
        assertThat(owner.getRole()).isEqualTo(UserRole.OWNER);
        assertThat(owner.isEnabled()).isTrue();
        assertThat(owner.getFailedLoginCount()).isZero();
        assertThat(passwordEncoder.matches("it_password_123", owner.getPasswordHash())).isTrue();
    }
}

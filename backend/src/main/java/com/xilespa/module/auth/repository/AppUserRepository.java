package com.xilespa.module.auth.repository;

import com.xilespa.module.auth.entity.AppUser;
import com.xilespa.module.auth.entity.UserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository truy xuất người dùng từ bảng {@code app_user}. */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByRole(UserRole role);

    boolean existsByEmailIgnoreCase(String email);
}

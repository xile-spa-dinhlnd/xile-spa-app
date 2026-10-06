package com.xilespa.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Múi giờ nghiệp vụ (BR-02). Mọi chỗ cần "bây giờ" hay "hôm nay" đều lấy từ {@link Clock} này. */
@Configuration(proxyBeanMethods = false)
public class TimeConfig {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }
}

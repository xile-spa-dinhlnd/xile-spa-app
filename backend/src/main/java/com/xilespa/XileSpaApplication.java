package com.xilespa;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class XileSpaApplication {

    public static void main(String[] args) {
        // Múi giờ MẶC ĐỊNH của JVM luôn là UTC, không phụ thuộc máy (Windows có thể báo
        // "Asia/Saigon",
        // tên mà PostgreSQL từ chối khi driver JDBC kết nối). Múi giờ NGHIỆP VỤ Asia/Ho_Chi_Minh
        // (BR-02)
        // không lấy từ đây mà từ Clock trong config/TimeConfig; code không dùng múi giờ mặc định.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(XileSpaApplication.class, args);
    }
}

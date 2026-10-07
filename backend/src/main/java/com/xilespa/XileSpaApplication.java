package com.xilespa;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class XileSpaApplication {

    public static void main(String[] args) {
        // Múi giờ nghiệp vụ toàn hệ thống là Asia/Ho_Chi_Minh (BR-02, AGENTS.md mục 4.7)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(XileSpaApplication.class, args);
    }
}

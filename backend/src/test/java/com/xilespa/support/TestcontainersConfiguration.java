package com.xilespa.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL thật cho test tích hợp (không dùng H2, xem {@code backend/AGENTS.md}). Image lấy theo
 * {@code postgres-image} trong {@code gradle/libs.versions.toml}. Dùng image Debian (không dùng
 * alpine) vì migration cần locale UTF-8 để lower() xử lý đúng chữ Việt.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        String image = System.getProperty("xile.test.postgres-image", "postgres:16");
        return new PostgreSQLContainer(DockerImageName.parse(image));
    }
}

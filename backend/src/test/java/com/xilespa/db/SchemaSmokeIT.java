package com.xilespa.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.xilespa.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Chạy bộ kiểm thử lược đồ {@code sql/schema_smoke_test.sql} trên PostgreSQL thật, sau khi Flyway
 * đã áp dụng mọi migration (S0-06). File dùng lệnh của psql ({@code \set}, {@code \pset}) nên được
 * chạy bằng psql bên trong container, không qua JDBC. Bộ kiểm thử tự ROLLBACK, không để lại dữ
 * liệu.
 */
@IntegrationTest
class SchemaSmokeIT {

    private static final String SCRIPT_IN_CONTAINER = "/tmp/schema_smoke_test.sql";

    @Autowired PostgreSQLContainer postgres;

    @Test
    void schemaSmokeTestPasses() throws Exception {
        postgres.copyFileToContainer(
                MountableFile.forClasspathResource("sql/schema_smoke_test.sql"),
                SCRIPT_IN_CONTAINER);

        ExecResult result =
                postgres.execInContainer(
                        "psql",
                        "-v",
                        "ON_ERROR_STOP=1",
                        "-U",
                        postgres.getUsername(),
                        "-d",
                        postgres.getDatabaseName(),
                        "-f",
                        SCRIPT_IN_CONTAINER);

        assertThat(result.getExitCode())
                .as(
                        "psql thoát với mã lỗi.%nstdout:%n%s%nstderr:%n%s",
                        result.getStdout(), result.getStderr())
                .isZero();
        assertThat(result.getStderr()).doesNotContain("ERROR");
        // Dòng cuối của script, chỉ in ra khi mọi kiểm tra đã qua.
        assertThat(result.getStdout()).contains("TẤT CẢ KIỂM TRA ĐÃ QUA");
    }
}

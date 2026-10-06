package com.xilespa.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xilespa.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gây lỗi thật từ trigger trên PostgreSQL (Testcontainers) và kiểm tra API trả lỗi tiếng Việt đúng
 * mã. Mỗi thao tác chạy trong transaction và bị rollback khi lỗi, không để lại dữ liệu.
 */
@IntegrationTest
@Import(DatabaseErrorMappingIT.TestController.class)
@WithMockUser
class DatabaseErrorMappingIT {

    @Autowired MockMvc mockMvc;

    @Test
    void writingToClosedDayIsRejected() throws Exception {
        mockMvc.perform(post("/test/db/closed-day"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DAY_CLOSED"))
                .andExpect(jsonPath("$.message").value(ErrorCode.DAY_CLOSED.defaultMessage()));
    }

    @Test
    void updatingAppendOnlyTableIsRejected() throws Exception {
        mockMvc.perform(post("/test/db/append-only"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RECORD_IMMUTABLE"));
    }

    @Test
    void closingFutureDayIsRejected() throws Exception {
        mockMvc.perform(post("/test/db/future-closing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FUTURE_DAY_CLOSING"))
                .andExpect(
                        jsonPath("$.message").value("Không thể chốt sổ cho ngày trong tương lai."));
    }

    @TestConfiguration
    @RestController
    static class TestController {

        private final JdbcTemplate jdbc;
        private final TransactionTemplate tx;

        TestController(JdbcTemplate jdbc, TransactionTemplate tx) {
            this.jdbc = jdbc;
            this.tx = tx;
        }

        @PostMapping("/test/db/closed-day")
        void closedDay() {
            tx.executeWithoutResult(
                    s -> {
                        long userId = insertUser();
                        closeDay("today_vn() - 1", userId);
                        jdbc.update(
                                """
                                INSERT INTO expense (business_date, category_id, amount, created_by)
                                VALUES (today_vn() - 1, (SELECT min(id) FROM expense_category), 50000, ?)
                                """,
                                userId);
                    });
        }

        @PostMapping("/test/db/append-only")
        void appendOnly() {
            tx.executeWithoutResult(
                    s -> {
                        closeDay("today_vn() - 1", insertUser());
                        jdbc.update(
                                "UPDATE daily_closing SET gross_revenue = 1"
                                        + " WHERE business_date = today_vn() - 1");
                    });
        }

        @PostMapping("/test/db/future-closing")
        void futureClosing() {
            tx.executeWithoutResult(s -> closeDay("today_vn() + 1", insertUser()));
        }

        private long insertUser() {
            return jdbc.queryForObject(
                    """
                    INSERT INTO app_user (email, password_hash, display_name)
                    VALUES ('test@xile.local', 'x', 'Test') RETURNING id
                    """,
                    Long.class);
        }

        private void closeDay(String dateExpression, long userId) {
            jdbc.update(
                    """
                    INSERT INTO daily_closing (business_date, gross_revenue, discount_total,
                        cash_collected, transfer_collected, visit_count, item_count,
                        expense_count, adjustment_count, closed_by)
                    VALUES (%s, 0, 0, 0, 0, 0, 0, 0, 0, ?)
                    """
                            .formatted(dateExpression),
                    userId);
        }
    }
}

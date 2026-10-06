package com.xilespa.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;

class DatabaseErrorsTest {

    @Test
    void translatesTriggerCodes() {
        assertThat(DatabaseErrors.translate(new SQLException("x", "XL001")))
                .contains(ErrorCode.DAY_CLOSED);
        assertThat(DatabaseErrors.translate(new SQLException("x", "XL002")))
                .contains(ErrorCode.RECORD_IMMUTABLE);
        assertThat(DatabaseErrors.translate(new SQLException("x", "XL004")))
                .contains(ErrorCode.FUTURE_DAY_CLOSING);
    }

    @Test
    void findsCodeDeepInCauseChain() {
        TransactionSystemException wrapped =
                new TransactionSystemException(
                        "commit thất bại",
                        new RuntimeException(new SQLException("Ngày đã chốt", "XL001")));

        assertThat(DatabaseErrors.translate(wrapped)).contains(ErrorCode.DAY_CLOSED);
    }

    @Test
    void otherDatabaseErrorsAreNotTranslated() {
        DataIntegrityViolationException uniqueViolation =
                new DataIntegrityViolationException(
                        "trùng", new SQLException("duplicate key", "23505"));

        assertThat(DatabaseErrors.translate(uniqueViolation)).isEmpty();
        assertThat(DatabaseErrors.translate(new IllegalStateException())).isEmpty();
    }
}

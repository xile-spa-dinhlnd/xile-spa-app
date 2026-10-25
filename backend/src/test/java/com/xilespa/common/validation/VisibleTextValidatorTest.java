package com.xilespa.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VisibleTextValidatorTest {

    private VisibleTextValidator validator;

    @BeforeEach
    void setUp() {
        validator = new VisibleTextValidator();
    }

    @Test
    void normalNameIsValid() {
        assertThat(validator.isValid("Lan A", null)).isTrue();
    }

    @Test
    void nameOfOnlyNbspIsRejected() {
        assertThat(validator.isValid("\u00A0\u00A0\u00A0", null)).isFalse();
    }

    @Test
    void nameOfOnlyZeroWidthIsRejected() {
        assertThat(validator.isValid("\u200B\u200C\uFEFF", null)).isFalse();
    }

    @Test
    void nullIsRejected() {
        assertThat(validator.isValid(null, null)).isFalse();
    }
}

package com.xilespa.common.validation;

import com.xilespa.common.util.TextNormalizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Kiểm tra chuỗi hợp lệ theo {@link TextNormalizer#normalizeName(String)}. */
public class VisibleTextValidator implements ConstraintValidator<VisibleText, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return !TextNormalizer.normalizeName(value).isEmpty();
    }
}

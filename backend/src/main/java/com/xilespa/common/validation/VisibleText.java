package com.xilespa.common.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Ràng buộc chuỗi phải chứa văn bản nhìn thấy được sau khi chuẩn hóa (S3-08, #2). Từ chối null,
 * chuỗi rỗng, chuỗi chỉ chứa khoảng trắng Unicode hoặc ký tự vô hình.
 */
@Documented
@Constraint(validatedBy = VisibleTextValidator.class)
@Target({FIELD, METHOD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface VisibleText {

    String message() default "Không được để trống";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

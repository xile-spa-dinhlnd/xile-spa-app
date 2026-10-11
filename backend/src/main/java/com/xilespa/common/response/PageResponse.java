package com.xilespa.common.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Định dạng chung cho mọi API trả danh sách có phân trang. Không trả thẳng {@link Page} của Spring
 * ra ngoài để định dạng JSON ổn định, không phụ thuộc phiên bản Spring.
 *
 * @param page số trang, bắt đầu từ 0
 */
public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}

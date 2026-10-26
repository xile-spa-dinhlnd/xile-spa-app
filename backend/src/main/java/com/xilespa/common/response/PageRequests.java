package com.xilespa.common.response;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Tiện ích dựng {@link PageRequest} an toàn chống tràn số và kẹp kích thước trang (S3-08, #6). */
public final class PageRequests {

    private PageRequests() {}

    /**
     * Dựng PageRequest an toàn:
     *
     * <ul>
     *   <li>{@code size} được kẹp trong đoạn {@code [1, maxSize]}.
     *   <li>{@code page} được kẹp trong đoạn {@code [0, Integer.MAX_VALUE / size]} để tích không
     *       tràn.
     * </ul>
     */
    public static PageRequest of(int page, int size, int maxSize, Sort sort) {
        int clampedSize = Math.clamp(size, 1, maxSize);
        int maxPage = Integer.MAX_VALUE / clampedSize;
        int clampedPage = Math.clamp(page, 0, maxPage);
        return PageRequest.of(clampedPage, clampedSize, sort);
    }
}

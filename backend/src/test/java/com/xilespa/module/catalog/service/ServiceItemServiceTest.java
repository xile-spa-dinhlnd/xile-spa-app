package com.xilespa.module.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceItemServiceTest {

    @Test
    void likePatternEscapesWildcards() {
        assertThat(ServiceItemService.likePattern(null)).isEqualTo("%");
        assertThat(ServiceItemService.likePattern("  ")).isEqualTo("%");
        assertThat(ServiceItemService.likePattern(" Gội ")).isEqualTo("%gội%");
        assertThat(ServiceItemService.likePattern("50%_a\\")).isEqualTo("%50\\%\\_a\\\\%");
    }

    @Test
    void likePatternNormalizesKeyword() {
        // Có NBSP, zero-width, khoảng trắng thừa
        assertThat(ServiceItemService.likePattern("\u00A0 GỘI\u200B   ĐẦU \u00A0"))
                .isEqualTo("%gội đầu%");
    }
}

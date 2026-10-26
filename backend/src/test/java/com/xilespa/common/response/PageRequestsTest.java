package com.xilespa.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class PageRequestsTest {

    @Test
    void normalPageIsKept() {
        PageRequest pr = PageRequests.of(2, 20, 50, Sort.by("id"));
        assertThat(pr.getPageNumber()).isEqualTo(2);
        assertThat(pr.getPageSize()).isEqualTo(20);
        assertThat(pr.getSort()).isEqualTo(Sort.by("id"));
    }

    @Test
    void negativePageBecomesFirst() {
        PageRequest pr = PageRequests.of(-5, 20, 50, Sort.unsorted());
        assertThat(pr.getPageNumber()).isEqualTo(0);
        assertThat(pr.getPageSize()).isEqualTo(20);
    }

    @Test
    void sizeIsClampedToMax() {
        PageRequest tooLarge = PageRequests.of(0, 100, 50, Sort.unsorted());
        assertThat(tooLarge.getPageSize()).isEqualTo(50);

        PageRequest tooSmall = PageRequests.of(0, 0, 50, Sort.unsorted());
        assertThat(tooSmall.getPageSize()).isEqualTo(1);

        PageRequest negativeSize = PageRequests.of(0, -10, 50, Sort.unsorted());
        assertThat(negativeSize.getPageSize()).isEqualTo(1);
    }

    @Test
    void hugePageDoesNotOverflow() {
        int maxSize = 50;
        int size = 20;
        PageRequest pr = PageRequests.of(Integer.MAX_VALUE, size, maxSize, Sort.unsorted());

        int expectedMaxPage = Integer.MAX_VALUE / size;
        assertThat(pr.getPageNumber()).isEqualTo(expectedMaxPage);
        assertThat(pr.getPageSize()).isEqualTo(size);
        // Kiểm tra không tràn khi tính offset
        assertThat((long) pr.getPageNumber() * pr.getPageSize())
                .isLessThanOrEqualTo(Integer.MAX_VALUE);
    }
}

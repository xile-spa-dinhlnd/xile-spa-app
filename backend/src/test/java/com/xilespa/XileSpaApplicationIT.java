package com.xilespa;

import static org.assertj.core.api.Assertions.assertThat;

import com.xilespa.support.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class XileSpaApplicationIT {

    @Autowired Flyway flyway;

    @Test
    void contextLoadsAndAppliesMigrations() {
        assertThat(flyway.info().applied())
                .extracting(m -> m.getVersion().getVersion())
                .containsExactly("1", "2");
    }
}

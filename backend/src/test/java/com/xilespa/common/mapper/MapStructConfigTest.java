package com.xilespa.common.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/** Kiểm tra MapStruct đã được cấu hình đúng: annotation processor chạy, map được record. */
class MapStructConfigTest {

    record Source(Long id, String name, long price) {}

    record Target(Long id, String name, long price) {}

    @Mapper(config = MapStructConfig.class)
    interface SampleMapper {
        Target toTarget(Source source);
    }

    @Test
    void generatedMapperCopiesAllFields() {
        SampleMapper mapper = Mappers.getMapper(SampleMapper.class);

        Target target = mapper.toTarget(new Source(1L, "Gội 30 phút", 69_000L));

        assertThat(target).isEqualTo(new Target(1L, "Gội 30 phút", 69_000L));
    }
}

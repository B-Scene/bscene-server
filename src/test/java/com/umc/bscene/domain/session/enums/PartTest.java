package com.umc.bscene.domain.session.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializeEtc() throws Exception {
        Part part = objectMapper.readValue("\"ETC\"", Part.class);

        assertThat(part).isEqualTo(Part.ETC);
    }
}

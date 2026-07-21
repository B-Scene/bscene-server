package com.umc.bscene.domain.session.converter;

import com.umc.bscene.domain.auth.enums.onboarding.Region;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.Arrays;

public class SessionRegionJsonDeserializer extends ValueDeserializer<Region> {

    @Override
    public Region deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException {
        return fromKorean(parser.getValueAsString());
    }

    public static Region fromKorean(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return Arrays.stream(Region.values())
                .filter(region -> region.getName().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "유효하지 않은 세션 활동 지역입니다: " + value));
    }
}

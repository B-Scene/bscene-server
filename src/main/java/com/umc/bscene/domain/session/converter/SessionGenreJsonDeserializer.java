package com.umc.bscene.domain.session.converter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.Arrays;

public class SessionGenreJsonDeserializer extends ValueDeserializer<Genre> {

    @Override
    public Genre deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException {
        return fromKorean(parser.getValueAsString());
    }

    public static Genre fromKorean(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return Arrays.stream(Genre.values())
                .filter(genre -> genre.getName().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "유효하지 않은 세션 장르입니다: " + value));
    }
}

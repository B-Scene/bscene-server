package com.umc.bscene.domain.session.converter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class SessionGenreJsonSerializer extends ValueSerializer<Genre> {

    @Override
    public void serialize(Genre value, JsonGenerator generator, SerializationContext context)
            throws JacksonException {
        generator.writeString(value.getName());
    }
}

package com.umc.bscene.domain.session.converter;

import com.umc.bscene.domain.auth.enums.onboarding.Region;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class SessionRegionJsonSerializer extends ValueSerializer<Region> {

    @Override
    public void serialize(Region value, JsonGenerator generator, SerializationContext context)
            throws JacksonException {
        generator.writeString(value.getName());
    }
}

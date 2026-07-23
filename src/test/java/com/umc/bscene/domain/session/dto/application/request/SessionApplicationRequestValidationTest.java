package com.umc.bscene.domain.session.dto.application.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.metadata.BeanDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionApplicationRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void applicationTextLengthsMatchPolicy() {
        assertApplicationLengths(MySessionApplicationCreateRequest.class);
        assertApplicationLengths(MySessionApplicationUpdateRequest.class);
    }

    @Test
    void careerTextLengthsMatchPolicy() {
        assertCareerLengths(MySessionApplicationCreateRequest.CareerRequest.class);
        assertCareerLengths(MySessionApplicationUpdateRequest.CareerRequest.class);
    }

    private void assertApplicationLengths(Class<?> type) {
        assertThat(maxSize(type, "title")).isEqualTo(50);
        assertThat(maxSize(type, "purpose")).isEqualTo(50);
        assertThat(maxSize(type, "oneLineIntro")).isEqualTo(50);
        assertThat(maxSize(type, "intro")).isEqualTo(500);
    }

    private void assertCareerLengths(Class<?> type) {
        assertThat(maxSize(type, "name")).isEqualTo(50);
        assertThat(maxSize(type, "period")).isEqualTo(50);
        assertThat(maxSize(type, "description")).isEqualTo(50);
    }

    private int maxSize(Class<?> type, String property) {
        BeanDescriptor descriptor = validator.getConstraintsForClass(type);
        return descriptor.getConstraintsForProperty(property).getConstraintDescriptors().stream()
                .filter(constraint -> constraint.getAnnotation()
                        .annotationType().getName()
                        .equals("jakarta.validation.constraints.Size"))
                .map(constraint -> (Integer) constraint.getAttributes().get("max"))
                .findFirst()
                .orElseThrow();
    }
}

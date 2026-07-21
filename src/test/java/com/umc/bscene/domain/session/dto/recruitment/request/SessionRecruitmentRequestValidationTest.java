package com.umc.bscene.domain.session.dto.recruitment.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.metadata.BeanDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionRecruitmentRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void titleIsNotBlankForCreateAndUpdate() {
        assertThat(hasConstraint(
                SessionRecruitmentCreateRequest.class,
                "recruitmentTitle",
                "jakarta.validation.constraints.NotBlank"
        )).isTrue();
        assertThat(hasConstraint(
                SessionRecruitmentUpdateRequest.class,
                "recruitmentTitle",
                "jakarta.validation.constraints.NotBlank"
        )).isTrue();
    }

    @Test
    void qualificationIsLimitedToFiveHundredCharactersForCreateAndUpdate() {
        assertThat(maxSize(SessionRecruitmentCreateRequest.class, "qualification"))
                .isEqualTo(500);
        assertThat(maxSize(SessionRecruitmentUpdateRequest.class, "qualification"))
                .isEqualTo(500);
    }

    private boolean hasConstraint(Class<?> type, String property, String annotationName) {
        BeanDescriptor descriptor = validator.getConstraintsForClass(type);
        return descriptor.getConstraintsForProperty(property).getConstraintDescriptors().stream()
                .anyMatch(constraint -> constraint.getAnnotation()
                        .annotationType().getName().equals(annotationName));
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

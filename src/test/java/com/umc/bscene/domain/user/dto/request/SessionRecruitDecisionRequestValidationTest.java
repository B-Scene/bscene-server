package com.umc.bscene.domain.user.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SessionRecruitDecisionRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void isApproved가_null이면_검증에_실패한다() {
        SessionRecruitDecisionRequest request = new SessionRecruitDecisionRequest(null);

        Set<ConstraintViolation<SessionRecruitDecisionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("isApproved"));
    }

    @Test
    void isApproved가_값이_있으면_검증을_통과한다() {
        SessionRecruitDecisionRequest request = new SessionRecruitDecisionRequest(true);

        Set<ConstraintViolation<SessionRecruitDecisionRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}

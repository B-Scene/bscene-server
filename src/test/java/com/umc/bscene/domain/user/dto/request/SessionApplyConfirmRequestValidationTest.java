package com.umc.bscene.domain.user.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SessionApplyConfirmRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void isAccepted가_null이면_검증에_실패한다() {
        SessionApplyConfirmRequest request = new SessionApplyConfirmRequest(
                null, null, null);

        Set<ConstraintViolation<SessionApplyConfirmRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("isAccepted"));
    }

    @Test
    void nickname이_31자면_검증에_실패한다() {
        SessionApplyConfirmRequest request = new SessionApplyConfirmRequest(
                true, "a".repeat(31), null);

        Set<ConstraintViolation<SessionApplyConfirmRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("nickname"));
    }

    @Test
    void nickname이_30자면_검증을_통과한다() {
        SessionApplyConfirmRequest request = new SessionApplyConfirmRequest(
                true, "a".repeat(30), null);

        Set<ConstraintViolation<SessionApplyConfirmRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("nickname"));
    }

    @Test
    void nickname이_null이면_검증을_통과한다() {
        SessionApplyConfirmRequest request = new SessionApplyConfirmRequest(
                true, null, null);

        Set<ConstraintViolation<SessionApplyConfirmRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("nickname"));
    }

    @Test
    void part이_null이어도_검증을_통과한다() {
        SessionApplyConfirmRequest request = new SessionApplyConfirmRequest(
                true, "닉네임", null);

        Set<ConstraintViolation<SessionApplyConfirmRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("part"));
    }
}

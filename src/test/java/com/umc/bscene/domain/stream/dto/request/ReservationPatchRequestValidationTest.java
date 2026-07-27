package com.umc.bscene.domain.stream.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationPatchRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void title이_공백만으로_채워지면_검증에_실패한다() {
        ReservationPatchRequest request = new ReservationPatchRequest(
                "   ", null, null, null, null);

        Set<ConstraintViolation<ReservationPatchRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void title이_null이면_변경_없음으로_간주되어_검증을_통과한다() {
        ReservationPatchRequest request = new ReservationPatchRequest(
                null, null, null, null, null);

        Set<ConstraintViolation<ReservationPatchRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void title이_51자면_검증에_실패한다() {
        ReservationPatchRequest request = new ReservationPatchRequest(
                "a".repeat(51), null, null, null, null);

        Set<ConstraintViolation<ReservationPatchRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void title이_50자면_검증을_통과한다() {
        ReservationPatchRequest request = new ReservationPatchRequest(
                "a".repeat(50), null, null, null, null);

        Set<ConstraintViolation<ReservationPatchRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void description이_101자면_검증에_실패한다() {
        ReservationPatchRequest request = new ReservationPatchRequest(
                null, "a".repeat(101), null, null, null);

        Set<ConstraintViolation<ReservationPatchRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void description이_100자면_검증을_통과한다() {
        ReservationPatchRequest request = new ReservationPatchRequest(
                null, "a".repeat(100), null, null, null);

        Set<ConstraintViolation<ReservationPatchRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void scheduledAt이_과거이면_검증에_실패한다() {
        ReservationPatchRequest request = new ReservationPatchRequest(
                null, null, null, LocalDateTime.now().minusDays(1), null);

        Set<ConstraintViolation<ReservationPatchRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("scheduledAt"));
    }

    @Test
    void scheduledAt이_미래이면_검증을_통과한다() {
        ReservationPatchRequest request = new ReservationPatchRequest(
                null, null, null, LocalDateTime.now().plusDays(1), null);

        Set<ConstraintViolation<ReservationPatchRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("scheduledAt"));
    }

    @Test
    void coHost가_11명이면_검증에_실패한다() {
        List<Long> coHost = new ArrayList<>();
        for (long i = 0; i < 11; i++) {
            coHost.add(i);
        }
        ReservationPatchRequest request = new ReservationPatchRequest(
                null, null, null, null, coHost);

        Set<ConstraintViolation<ReservationPatchRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("coHost"));
    }

    @Test
    void coHost가_10명이면_검증을_통과한다() {
        List<Long> coHost = new ArrayList<>();
        for (long i = 0; i < 10; i++) {
            coHost.add(i);
        }
        ReservationPatchRequest request = new ReservationPatchRequest(
                null, null, null, null, coHost);

        Set<ConstraintViolation<ReservationPatchRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void coHost에_null_원소가_있으면_검증에_실패한다() {
        List<Long> coHost = new ArrayList<>();
        coHost.add(1L);
        coHost.add(null);
        ReservationPatchRequest request = new ReservationPatchRequest(
                null, null, null, null, coHost);

        Set<ConstraintViolation<ReservationPatchRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("coHost[1].<list element>"));
    }
}

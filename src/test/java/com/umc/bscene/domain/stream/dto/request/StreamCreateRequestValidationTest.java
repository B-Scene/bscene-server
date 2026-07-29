package com.umc.bscene.domain.stream.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StreamCreateRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void title이_공백이면_검증에_실패한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                " ", null, null, null, null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void title이_51자면_검증에_실패한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                "a".repeat(51), null, null, null, null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void title이_50자면_검증을_통과한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                "a".repeat(50), null, null, null, null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void description이_101자면_검증에_실패한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                "title", "a".repeat(101), null, null, null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void description이_100자면_검증을_통과한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                "title", "a".repeat(100), null, null, null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void description이_null이면_검증을_통과한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                "title", null, null, null, null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void thumbnailImageUrl이_256자면_검증에_실패한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                "title", null, "a".repeat(256), null, null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("thumbnailImageUrl"));
    }

    @Test
    void thumbnailImageUrl이_255자면_검증을_통과한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                "title", null, "a".repeat(255), null, null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("thumbnailImageUrl"));
    }

    @Test
    void scheduledAt이_과거이면_검증에_실패한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                "title", null, null, LocalDateTime.now().minusDays(1), null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("scheduledAt"));
    }

    @Test
    void scheduledAt이_미래이면_검증을_통과한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                "title", null, null, LocalDateTime.now().plusDays(1), null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("scheduledAt"));
    }

    @Test
    void scheduledAt이_null이면_검증을_통과한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                "title", null, null, null, null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("scheduledAt"));
    }

    @Test
    void 모든_필드가_유효하면_검증을_통과한다() {
        StreamCreateRequest request = new StreamCreateRequest(
                "title", "description", "thumbnailImageUrl", LocalDateTime.now().plusDays(1), null);

        Set<ConstraintViolation<StreamCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}

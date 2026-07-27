package com.umc.bscene.domain.stream.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MtxAuthRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void password가_비어있으면_검증에_실패한다() {
        MtxAuthRequest request = new MtxAuthRequest(
                "user", "", "ip", "publish", "path", "protocol", "query");

        Set<ConstraintViolation<MtxAuthRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void action이_publish이면_검증을_통과한다() {
        MtxAuthRequest request = new MtxAuthRequest(
                "user", "password", "ip", "publish", "path", "protocol", "query");

        Set<ConstraintViolation<MtxAuthRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("action"));
    }

    @Test
    void action이_read이면_검증을_통과한다() {
        MtxAuthRequest request = new MtxAuthRequest(
                "user", "password", "ip", "read", "path", "protocol", "query");

        Set<ConstraintViolation<MtxAuthRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("action"));
    }

    @Test
    void action이_허용되지_않은_값이면_검증에_실패한다() {
        MtxAuthRequest request = new MtxAuthRequest(
                "user", "password", "ip", "delete", "path", "protocol", "query");

        Set<ConstraintViolation<MtxAuthRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("action"));
    }

    @Test
    void path에_언더스코어가_포함되면_검증에_실패한다() {
        MtxAuthRequest request = new MtxAuthRequest(
                "user", "password", "ip", "publish", "my_path", "protocol", "query");

        Set<ConstraintViolation<MtxAuthRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("path"));
    }

    @Test
    void path에_슬래시가_포함되면_검증에_실패한다() {
        MtxAuthRequest request = new MtxAuthRequest(
                "user", "password", "ip", "publish", "my/path", "protocol", "query");

        Set<ConstraintViolation<MtxAuthRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("path"));
    }

    @Test
    void path가_65자면_검증에_실패한다() {
        MtxAuthRequest request = new MtxAuthRequest(
                "user", "password", "ip", "publish", "a".repeat(65), "protocol", "query");

        Set<ConstraintViolation<MtxAuthRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("path"));
    }

    @Test
    void path가_64자면_검증을_통과한다() {
        MtxAuthRequest request = new MtxAuthRequest(
                "user", "password", "ip", "publish", "a".repeat(64), "protocol", "query");

        Set<ConstraintViolation<MtxAuthRequest>> violations = validator.validate(request);

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("path"));
    }
}

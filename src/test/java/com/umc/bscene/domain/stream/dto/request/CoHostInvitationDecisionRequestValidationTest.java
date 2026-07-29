package com.umc.bscene.domain.stream.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CoHostInvitationDecisionRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void isAccepted가_null이면_검증에_실패한다() {
        CoHostInvitationDecisionRequest request =
                new CoHostInvitationDecisionRequest(null);

        Set<ConstraintViolation<CoHostInvitationDecisionRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("isAccepted");
                    assertThat(violation.getMessage())
                            .isEqualTo("공동 진행자 초대 수락·거절 값은 비어있을 수 없습니다.");
                });
    }

    @Test
    void isAccepted가_true나_false이면_검증을_통과한다() {
        assertThat(validator.validate(
                new CoHostInvitationDecisionRequest(true)
        )).isEmpty();
        assertThat(validator.validate(
                new CoHostInvitationDecisionRequest(false)
        )).isEmpty();
    }
}

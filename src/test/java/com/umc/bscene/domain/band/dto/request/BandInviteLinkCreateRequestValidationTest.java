package com.umc.bscene.domain.band.dto.request;

import com.umc.bscene.domain.band.enums.BandMemberType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BandInviteLinkCreateRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void memberType이_null이면_검증에_실패한다() {
        BandInviteLinkCreateRequest request = new BandInviteLinkCreateRequest(null);

        Set<ConstraintViolation<BandInviteLinkCreateRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath().toString().equals("memberType"));
    }

    @Test
    void memberType이_MEMBER이면_검증을_통과한다() {
        BandInviteLinkCreateRequest request =
                new BandInviteLinkCreateRequest(BandMemberType.MEMBER);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void memberType이_SESSION이면_검증을_통과한다() {
        BandInviteLinkCreateRequest request =
                new BandInviteLinkCreateRequest(BandMemberType.SESSION);

        assertThat(validator.validate(request)).isEmpty();
    }
}

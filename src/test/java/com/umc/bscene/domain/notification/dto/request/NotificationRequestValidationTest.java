package com.umc.bscene.domain.notification.dto.request;

import com.umc.bscene.domain.notification.enums.PushPlatform;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private <T> void assertInvalidProperty(T request, String property) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath().toString().equals(property));
    }

    @Nested
    class PushTokenSave {

        @Test
        void token이_null이거나_공백이면_검증에_실패한다() {
            assertInvalidProperty(
                    new PushTokenSaveRequest(null, PushPlatform.WEB),
                    "token"
            );
            assertInvalidProperty(
                    new PushTokenSaveRequest(" ", PushPlatform.WEB),
                    "token"
            );
        }

        @Test
        void platform이_null이면_검증에_실패한다() {
            assertInvalidProperty(
                    new PushTokenSaveRequest("push-token", null),
                    "platform"
            );
        }

        @Test
        void 모든_필드가_유효하면_검증을_통과한다() {
            PushTokenSaveRequest request =
                    new PushTokenSaveRequest("push-token", PushPlatform.WEB);

            assertThat(validator.validate(request)).isEmpty();
        }
    }

    @Nested
    class PushTokenDelete {

        @Test
        void token이_null이거나_공백이면_검증에_실패한다() {
            assertInvalidProperty(new PushTokenDeleteRequest(null), "token");
            assertInvalidProperty(new PushTokenDeleteRequest(" "), "token");
        }

        @Test
        void token이_있으면_검증을_통과한다() {
            assertThat(validator.validate(
                    new PushTokenDeleteRequest("push-token")
            )).isEmpty();
        }
    }

    @Nested
    class PushTestSend {

        @Test
        void title이_null이거나_공백이면_검증에_실패한다() {
            assertInvalidProperty(
                    new PushTestSendRequest(null, "본문"),
                    "title"
            );
            assertInvalidProperty(
                    new PushTestSendRequest(" ", "본문"),
                    "title"
            );
        }

        @Test
        void body가_null이거나_공백이면_검증에_실패한다() {
            assertInvalidProperty(
                    new PushTestSendRequest("제목", null),
                    "body"
            );
            assertInvalidProperty(
                    new PushTestSendRequest("제목", " "),
                    "body"
            );
        }

        @Test
        void 모든_필드가_유효하면_검증을_통과한다() {
            assertThat(validator.validate(
                    new PushTestSendRequest("제목", "본문")
            )).isEmpty();
        }
    }

    @Nested
    class NotificationSettingUpdate {

        @Test
        void enabled가_null이면_검증에_실패한다() {
            assertInvalidProperty(
                    new NotificationSettingUpdateRequest(null),
                    "enabled"
            );
        }

        @Test
        void enabled가_true나_false이면_검증을_통과한다() {
            assertThat(validator.validate(
                    new NotificationSettingUpdateRequest(true)
            )).isEmpty();
            assertThat(validator.validate(
                    new NotificationSettingUpdateRequest(false)
            )).isEmpty();
        }
    }
}

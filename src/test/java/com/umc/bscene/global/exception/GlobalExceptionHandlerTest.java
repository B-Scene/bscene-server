package com.umc.bscene.global.exception;

import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GlobalExceptionHandler 단위 테스트.
 * <p>
 * @ExceptionHandler 메소드를 Spring 컨텍스트 없이 평범한 자바 메소드로 직접 호출해
 * 반환된 ResponseEntity의 상태/코드/메시지를 검증한다.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** MethodParameter 생성을 위한 더미 시그니처. */
    @SuppressWarnings("unused")
    private void dummyMethod(String name) {
    }

    private MethodParameter dummyParameter() throws NoSuchMethodException {
        return new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class), 0
        );
    }

    /** 필드 에러가 담긴 실제 BindingResult (deep mock 대신 실물 사용). */
    private BeanPropertyBindingResult bindingResultWithFieldErrors(FieldError... fieldErrors) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        for (FieldError fieldError : fieldErrors) {
            bindingResult.addError(fieldError);
        }
        return bindingResult;
    }

    @Nested
    @DisplayName("@Valid 검증 실패 (MethodArgumentNotValidException)")
    class MethodArgumentNotValid {

        @Test
        @DisplayName("400 COMMON_400_2와 첫 번째 필드 에러 메시지를 그대로 노출한다")
        void surfacesFirstFieldErrorMessage() throws Exception {
            BeanPropertyBindingResult bindingResult = bindingResultWithFieldErrors(
                    new FieldError("request", "title", "제목은 필수입니다."),
                    new FieldError("request", "description", "설명은 200자 이하여야 합니다.")
            );
            MethodArgumentNotValidException exception =
                    new MethodArgumentNotValidException(dummyParameter(), bindingResult);

            ResponseEntity<ErrorResponse<?>> response = handler.handleMethodArgumentNotValidException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getIsSuccess()).isFalse();
            assertThat(body.getStatus()).isEqualTo(400);
            assertThat(body.getCode()).isEqualTo(GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY.getCode());
            assertThat(body.getMessage()).isEqualTo("제목은 필수입니다.");
            assertThat(body.getResult()).isNull();
        }

        @Test
        @DisplayName("필드 에러 메시지가 null이면 message도 null로 내려간다")
        void allowsNullDefaultMessage() throws Exception {
            BeanPropertyBindingResult bindingResult = bindingResultWithFieldErrors(
                    new FieldError("request", "title", null)
            );
            MethodArgumentNotValidException exception =
                    new MethodArgumentNotValidException(dummyParameter(), bindingResult);

            ResponseEntity<ErrorResponse<?>> response = handler.handleMethodArgumentNotValidException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isNull();
            assertThat(response.getBody().getCode())
                    .isEqualTo(GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY.getCode());
        }
    }

    @Nested
    @DisplayName("바인딩 실패 (BindException)")
    class Bind {

        @Test
        @DisplayName("400 COMMON_400_2와 필드 에러 메시지를 그대로 노출한다")
        void surfacesFieldErrorMessage() {
            BindException exception = new BindException(bindingResultWithFieldErrors(
                    new FieldError("request", "page", "page는 0 이상이어야 합니다.")
            ));

            ResponseEntity<ErrorResponse<?>> response = handler.handleBindException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getIsSuccess()).isFalse();
            assertThat(body.getStatus()).isEqualTo(400);
            assertThat(body.getCode()).isEqualTo(GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY.getCode());
            assertThat(body.getMessage()).isEqualTo("page는 0 이상이어야 합니다.");
        }

        @Test
        @DisplayName("필드 에러가 하나도 없으면 getFieldError()가 null이라 NPE가 난다 (현재 구현 그대로)")
        void throwsWhenNoFieldError() {
            BindException exception = new BindException(bindingResultWithFieldErrors());

            assertThatThrownBy(() -> handler.handleBindException(exception))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("요청 바디 파싱 실패 (HttpMessageNotReadableException)")
    class HttpMessageNotReadable {

        @Test
        @DisplayName("400 COMMON_400_2 기본 메시지를 반환하고 파싱 예외 원문을 노출하지 않는다")
        void returnsMaskedBadRequest() {
            HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                    "JSON parse error: Unexpected character at [Source: (secret-payload)]",
                    new MockHttpInputMessage(new byte[0])
            );

            ResponseEntity<ErrorResponse<?>> response = handler.handleHttpMessageNotReadableException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getCode()).isEqualTo(GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY.getCode());
            assertThat(body.getMessage()).isEqualTo(GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY.getMessage());
            assertThat(body.getMessage()).doesNotContain("secret-payload");
        }
    }

    @Nested
    @DisplayName("파라미터 타입 불일치 (MethodArgumentTypeMismatchException)")
    class TypeMismatch {

        @Test
        @DisplayName("400 COMMON_400_3 기본 메시지를 반환한다")
        void returnsInvalidParameter() throws Exception {
            MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                    "not-a-number", Long.class, "streamId", dummyParameter(),
                    new IllegalArgumentException("For input string: \"not-a-number\"")
            );

            ResponseEntity<ErrorResponse<?>> response = handler.handleMethodArgumentTypeMismatchException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(400);
            assertThat(body.getCode()).isEqualTo(GeneralErrorCode.INVALID_HTTP_MESSAGE_PARAMETER.getCode());
            assertThat(body.getMessage()).isEqualTo(GeneralErrorCode.INVALID_HTTP_MESSAGE_PARAMETER.getMessage());
        }
    }

    @Nested
    @DisplayName("필수 파라미터 누락 (MissingServletRequestParameterException)")
    class MissingParameter {

        @Test
        @DisplayName("400 COMMON_400_3과 누락된 파라미터 이름이 포함된 메시지를 반환한다")
        void includesParameterName() {
            MissingServletRequestParameterException exception =
                    new MissingServletRequestParameterException("cursor", "Long");

            ResponseEntity<ErrorResponse<?>> response =
                    handler.handleMissingServletRequestParameterException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getCode()).isEqualTo(GeneralErrorCode.INVALID_HTTP_MESSAGE_PARAMETER.getCode());
            assertThat(body.getMessage()).isEqualTo("cursor 파라미터가 필요합니다.");
        }
    }

    @Nested
    @DisplayName("멀티파트 파트 누락 (MissingServletRequestPartException)")
    class MissingPart {

        @Test
        @DisplayName("400 COMMON_400_3 기본 메시지를 반환한다")
        void returnsInvalidParameter() {
            MissingServletRequestPartException exception = new MissingServletRequestPartException("file");

            ResponseEntity<ErrorResponse<?>> response =
                    handler.handleMissingServletRequestPartException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getCode()).isEqualTo(GeneralErrorCode.INVALID_HTTP_MESSAGE_PARAMETER.getCode());
            assertThat(body.getMessage()).isEqualTo(GeneralErrorCode.INVALID_HTTP_MESSAGE_PARAMETER.getMessage());
        }
    }

    @Nested
    @DisplayName("지원하지 않는 HTTP 메소드 (HttpRequestMethodNotSupportedException)")
    class UnsupportedMethod {

        @Test
        @DisplayName("405 COMMON_405_1을 반환한다")
        void returnsMethodNotAllowed() {
            HttpRequestMethodNotSupportedException exception =
                    new HttpRequestMethodNotSupportedException("PATCH");

            ResponseEntity<ErrorResponse<?>> response =
                    handler.handleHttpRequestMethodNotSupportedException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(405);
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(405);
            assertThat(body.getCode()).isEqualTo(GeneralErrorCode.UNSUPPORTED_HTTP_METHOD.getCode());
            assertThat(body.getMessage()).isEqualTo(GeneralErrorCode.UNSUPPORTED_HTTP_METHOD.getMessage());
        }
    }

    @Nested
    @DisplayName("존재하지 않는 엔드포인트")
    class NotFound {

        @Test
        @DisplayName("NoHandlerFoundException은 404 COMMON_404_1을 반환한다")
        void noHandlerFound() {
            NoHandlerFoundException exception =
                    new NoHandlerFoundException("GET", "/not-exists", new HttpHeaders());

            ResponseEntity<ErrorResponse<?>> response = handler.handleNoHandlerFoundException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(404);
            assertThat(body.getCode()).isEqualTo(GeneralErrorCode.NOT_FOUND_ENDPOINT.getCode());
            assertThat(body.getMessage()).isEqualTo(GeneralErrorCode.NOT_FOUND_ENDPOINT.getMessage());
        }

        @Test
        @DisplayName("NoResourceFoundException도 404 COMMON_404_1을 반환한다")
        void noResourceFound() {
            NoResourceFoundException exception =
                    new NoResourceFoundException(HttpMethod.GET, "/static/missing.png", "missing.png");

            ResponseEntity<ErrorResponse<?>> response = handler.handleNoResourceFoundException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getCode()).isEqualTo(GeneralErrorCode.NOT_FOUND_ENDPOINT.getCode());
            assertThat(body.getMessage()).isEqualTo(GeneralErrorCode.NOT_FOUND_ENDPOINT.getMessage());
        }
    }

    @Nested
    @DisplayName("비동기 응답 중단 (AsyncRequestNotUsableException)")
    class AsyncNotUsable {

        @Test
        @DisplayName("클라이언트 연결 종료는 정상 이탈로 보고 아무 응답도 만들지 않는다")
        void swallowsBrokenPipe() {
            AsyncRequestNotUsableException exception =
                    new AsyncRequestNotUsableException("ServletOutputStream failed to write: Broken pipe");

            assertThatCode(() -> handler.handleAsyncRequestNotUsableException(exception))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("비즈니스 예외 (BaseException)")
    class Base {

        @Test
        @DisplayName("도메인 에러 코드의 status/code/message가 그대로 응답에 매핑된다")
        void mapsDomainErrorCode() {
            BaseException exception = new BaseException(StreamErrorCode.ALREADY_LIVE);
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/lives");
            request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            ResponseEntity<ErrorResponse<?>> response = handler.handleBaseException(exception, request);

            assertThat(response.getStatusCode().value()).isEqualTo(409);
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getIsSuccess()).isFalse();
            assertThat(body.getStatus()).isEqualTo(StreamErrorCode.ALREADY_LIVE.getStatus());
            assertThat(body.getCode()).isEqualTo("LIVE409_1");
            assertThat(body.getMessage()).isEqualTo(StreamErrorCode.ALREADY_LIVE.getMessage());
            assertThat(body.getResult()).isNull();
        }

        @ParameterizedTest
        @EnumSource(StreamErrorCode.class)
        @DisplayName("모든 StreamErrorCode가 자신의 status/code/message로 매핑된다")
        void mapsEveryStreamErrorCode(StreamErrorCode errorCode) {
            BaseException exception = new BaseException(errorCode);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lives/1");

            ResponseEntity<ErrorResponse<?>> response = handler.handleBaseException(exception, request);

            assertThat(response.getStatusCode().value()).isEqualTo(errorCode.getStatus());
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(errorCode.getStatus());
            assertThat(body.getCode()).isEqualTo(errorCode.getCode());
            assertThat(body.getMessage()).isEqualTo(errorCode.getMessage());
        }

        @Test
        @DisplayName("Accept 헤더가 없어도 JSON 바디를 담아 반환한다")
        void returnsBodyWhenAcceptHeaderAbsent() {
            BaseException exception = new BaseException(StreamErrorCode.REPLAY_NOT_FOUND);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/replays/1");

            ResponseEntity<ErrorResponse<?>> response = handler.handleBaseException(exception, request);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("LIVE404_3");
        }

        @Test
        @DisplayName("SSE(text/event-stream) 요청에는 상태 코드만 반환하고 바디를 비운다")
        void returnsStatusOnlyForSseRequest() {
            BaseException exception = new BaseException(StreamErrorCode.FORBIDDEN_REQUEST);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lives/1/viewers");
            request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE);

            ResponseEntity<ErrorResponse<?>> response = handler.handleBaseException(exception, request);

            assertThat(response.getStatusCode().value()).isEqualTo(403);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("Accept에 text/event-stream이 다른 타입과 함께 있어도 바디를 비운다")
        void returnsStatusOnlyWhenSseAmongMultipleAcceptTypes() {
            BaseException exception = new BaseException(StreamErrorCode.AUDIO_STREAM_NOT_LIVE);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lives/1/viewers");
            request.addHeader(HttpHeaders.ACCEPT,
                    MediaType.APPLICATION_JSON_VALUE + ", " + MediaType.TEXT_EVENT_STREAM_VALUE);

            ResponseEntity<ErrorResponse<?>> response = handler.handleBaseException(exception, request);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("BaseException 하위 클래스도 동일하게 매핑된다")
        void mapsSubclass() {
            class StreamException extends BaseException {
                StreamException() {
                    super(StreamErrorCode.NO_ACTIVE_BAND_PROFILE);
                }
            }
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/lives");

            ResponseEntity<ErrorResponse<?>> response = handler.handleBaseException(new StreamException(), request);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("LIVE400_1");
            assertThat(response.getBody().getMessage())
                    .isEqualTo(StreamErrorCode.NO_ACTIVE_BAND_PROFILE.getMessage());
        }
    }

    @Nested
    @DisplayName("최후의 보루 (Exception)")
    class CatchAll {

        @Test
        @DisplayName("500 COMMON_500_1을 반환하고 원본 예외 메시지를 노출하지 않는다")
        void masksRawExceptionMessage() {
            Exception exception = new IllegalStateException("jdbc:mysql://prod-db:3306 connection refused");

            ResponseEntity<ErrorResponse<?>> response = handler.handleException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            ErrorResponse<?> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getIsSuccess()).isFalse();
            assertThat(body.getStatus()).isEqualTo(500);
            assertThat(body.getCode()).isEqualTo(GeneralErrorCode.SERVER_ERROR.getCode());
            assertThat(body.getMessage()).isEqualTo(GeneralErrorCode.SERVER_ERROR.getMessage());
            assertThat(body.getMessage()).doesNotContain("prod-db");
            assertThat(body.getResult()).isNull();
        }

        @Test
        @DisplayName("메시지가 null인 예외도 500 COMMON_500_1로 처리한다")
        void handlesNullMessage() {
            ResponseEntity<ErrorResponse<?>> response = handler.handleException(new RuntimeException());

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(GeneralErrorCode.SERVER_ERROR.getCode());
        }
    }
}

package com.umc.bscene.global.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import com.umc.bscene.global.response.code.GeneralSuccessCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 응답 엔벨로프(ApiResponse / SuccessResponse / ErrorResponse) 단위 테스트.
 * <p>
 * JSON 필드 이름은 클라이언트와의 공개 계약이므로 직렬화 결과까지 고정한다.
 */
@DisplayName("응답 엔벨로프")
class ApiResponseTest {

    private static final String TIMESTAMP_PATTERN = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 직렬화 결과를 JsonNode로 되돌려 준다. */
    private JsonNode serialize(Object response) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(response));
    }

    /** JSON 최상위 필드 이름 목록. */
    private List<String> fieldNamesOf(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    record Payload(Long id, String name) {
    }

    @Nested
    @DisplayName("ApiResponse")
    class ApiResponseSpec {

        @Test
        @DisplayName("of(isSuccess, code)는 코드의 code/message를 그대로 담는다")
        void ofResponseCode() {
            ApiResponse response = ApiResponse.of(true, GeneralSuccessCode.SUCCESS_OK);

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getCode()).isEqualTo("COMMON_200");
            assertThat(response.getMessage()).isEqualTo(GeneralSuccessCode.SUCCESS_OK.getMessage());
        }

        @Test
        @DisplayName("of(isSuccess, code, message)는 코드의 message 대신 전달된 message를 쓴다")
        void ofResponseCodeWithOverriddenMessage() {
            ApiResponse response = ApiResponse.of(false, GeneralErrorCode.BAD_REQUEST_ERROR, "직접 지정한 메시지");

            assertThat(response.getIsSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo("COMMON_400_1");
            assertThat(response.getMessage()).isEqualTo("직접 지정한 메시지");
        }

        @Test
        @DisplayName("of(isSuccess, code문자열, message)는 전달값을 그대로 담는다")
        void ofRawStrings() {
            ApiResponse response = ApiResponse.of(true, "CUSTOM_200", "커스텀 메시지");

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getCode()).isEqualTo("CUSTOM_200");
            assertThat(response.getMessage()).isEqualTo("커스텀 메시지");
        }

        @Test
        @DisplayName("of(code) 단일 인자는 항상 실패 응답이다")
        void ofResponseCodeOnlyIsAlwaysFailure() {
            ApiResponse response = ApiResponse.of(GeneralErrorCode.UNAUTHORIZED_ERROR);

            assertThat(response.getIsSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo("COMMON_401_1");
            assertThat(response.getMessage()).isEqualTo(GeneralErrorCode.UNAUTHORIZED_ERROR.getMessage());
        }

        @Test
        @DisplayName("code/message가 null이어도 생성되고 NPE 없이 직렬화된다")
        void allowsNullCodeAndMessage() throws Exception {
            ApiResponse response = ApiResponse.of(null, (String) null, null);

            assertThat(response.getIsSuccess()).isNull();

            JsonNode json = serialize(response);
            assertThat(json.get("isSuccess").isNull()).isTrue();
            assertThat(json.get("code").isNull()).isTrue();
            assertThat(json.get("message").isNull()).isTrue();
        }

        @Test
        @DisplayName("timeStamp는 yyyy-MM-dd HH:mm:ss 포맷으로 자동 생성된다")
        void generatesFormattedTimeStamp() {
            ApiResponse response = ApiResponse.of(GeneralErrorCode.SERVER_ERROR);

            assertThat(response.getTimeStamp()).matches(TIMESTAMP_PATTERN);
        }

        @Test
        @DisplayName("JSON 필드는 isSuccess/code/message/timeStamp 네 개다")
        void pinsJsonFieldNames() throws Exception {
            JsonNode json = serialize(ApiResponse.of(true, GeneralSuccessCode.SUCCESS_OK));

            assertThat(fieldNamesOf(json))
                    .containsExactlyInAnyOrder("isSuccess", "code", "message", "timeStamp");
            assertThat(json.get("isSuccess").asBoolean()).isTrue();
            assertThat(json.get("code").asText()).isEqualTo("COMMON_200");
            assertThat(json.get("message").asText()).isEqualTo(GeneralSuccessCode.SUCCESS_OK.getMessage());
            assertThat(json.get("timeStamp").asText()).matches(TIMESTAMP_PATTERN);
        }
    }

    @Nested
    @DisplayName("SuccessResponse")
    class SuccessResponseSpec {

        @Test
        @DisplayName("ok()는 200 COMMON_200과 전달된 result를 담는다")
        void ok() {
            SuccessResponse<Payload> response = SuccessResponse.ok(new Payload(1L, "라이브"));

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getCode()).isEqualTo("COMMON_200");
            assertThat(response.getMessage()).isEqualTo(GeneralSuccessCode.SUCCESS_OK.getMessage());
            assertThat(response.getResult()).isEqualTo(new Payload(1L, "라이브"));
        }

        @Test
        @DisplayName("created()는 201 COMMON_201과 전달된 result를 담는다")
        void created() {
            SuccessResponse<Long> response = SuccessResponse.created(42L);

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.getCode()).isEqualTo("COMMON_201");
            assertThat(response.getResult()).isEqualTo(42L);
        }

        @Test
        @DisplayName("accepted()는 202 COMMON_202이고 result가 null이다")
        void accepted() {
            SuccessResponse<Void> response = SuccessResponse.accepted();

            assertThat(response.getStatus()).isEqualTo(202);
            assertThat(response.getCode()).isEqualTo("COMMON_202");
            assertThat(response.getResult()).isNull();
        }

        @Test
        @DisplayName("empty()는 인자를 무시하고 204 COMMON_204에 result=null을 담는다")
        void emptyDiscardsArgument() {
            SuccessResponse<String> response = SuccessResponse.empty("무시되는 값");

            assertThat(response.getStatus()).isEqualTo(204);
            assertThat(response.getCode()).isEqualTo("COMMON_204");
            assertThat(response.getResult()).isNull();
        }

        @Test
        @DisplayName("of()는 임의의 성공 코드와 데이터를 조합한다")
        void of() {
            SuccessResponse<List<String>> response =
                    SuccessResponse.of(List.of("a", "b"), GeneralSuccessCode.SUCCESS_CREATED);

            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.getResult()).containsExactly("a", "b");
        }

        @Test
        @DisplayName("빌더로도 동일한 응답을 만든다")
        void builder() {
            SuccessResponse<String> response = SuccessResponse.<String>builder()
                    .result("ok")
                    .baseResponseCode(GeneralSuccessCode.SUCCESS_OK)
                    .build();

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getResult()).isEqualTo("ok");
        }

        @Test
        @DisplayName("result가 null이어도 NPE 없이 직렬화된다")
        void serializesNullResult() {
            assertThatCode(() -> objectMapper.writeValueAsString(SuccessResponse.ok(null)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("JSON 필드는 isSuccess/status/code/message/result/timeStamp 여섯 개다")
        void pinsJsonFieldNames() throws Exception {
            JsonNode json = serialize(SuccessResponse.ok(new Payload(7L, "밴드")));

            assertThat(fieldNamesOf(json))
                    .containsExactlyInAnyOrder("isSuccess", "status", "code", "message", "result", "timeStamp");
            assertThat(json.get("isSuccess").asBoolean()).isTrue();
            assertThat(json.get("status").asInt()).isEqualTo(200);
            assertThat(json.get("code").asText()).isEqualTo("COMMON_200");
            assertThat(json.get("message").asText()).isEqualTo(GeneralSuccessCode.SUCCESS_OK.getMessage());
            assertThat(json.get("result").get("id").asLong()).isEqualTo(7L);
            assertThat(json.get("result").get("name").asText()).isEqualTo("밴드");
            assertThat(json.get("timeStamp").asText()).matches(TIMESTAMP_PATTERN);
        }

        @Test
        @DisplayName("result가 null이면 JSON에도 null로 남는다 (필드 자체는 유지)")
        void keepsNullResultField() throws Exception {
            JsonNode json = serialize(SuccessResponse.accepted());

            assertThat(json.has("result")).isTrue();
            assertThat(json.get("result").isNull()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(GeneralSuccessCode.class)
        @DisplayName("모든 성공 코드가 status/code/message로 그대로 매핑된다")
        void mapsEverySuccessCode(GeneralSuccessCode code) {
            SuccessResponse<String> response = SuccessResponse.of("data", code);

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo(code.getStatus());
            assertThat(response.getCode()).isEqualTo(code.getCode());
            assertThat(response.getMessage()).isEqualTo(code.getMessage());
        }
    }

    @Nested
    @DisplayName("ErrorResponse")
    class ErrorResponseSpec {

        @Test
        @DisplayName("from()은 isSuccess=false에 코드의 status/code/message를 담고 result는 null이다")
        void from() {
            ErrorResponse<Void> response = ErrorResponse.from(GeneralErrorCode.UNAUTHORIZED_ERROR);

            assertThat(response.getIsSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getCode()).isEqualTo("COMMON_401_1");
            assertThat(response.getMessage()).isEqualTo(GeneralErrorCode.UNAUTHORIZED_ERROR.getMessage());
            assertThat(response.getResult()).isNull();
        }

        @Test
        @DisplayName("of(code, message)는 status/code는 유지하고 message만 덮어쓴다")
        void ofWithMessage() {
            ErrorResponse<Void> response =
                    ErrorResponse.<Void>of(GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY, "제목은 필수입니다.");

            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getCode()).isEqualTo("COMMON_400_2");
            assertThat(response.getMessage()).isEqualTo("제목은 필수입니다.");
            assertThat(response.getResult()).isNull();
        }

        @Test
        @DisplayName("of(code, data)는 result에 데이터를 담고 message는 코드 기본값을 쓴다")
        void ofWithData() {
            ErrorResponse<Payload> response =
                    ErrorResponse.of(StreamErrorCode.INVALID_CO_HOST, new Payload(3L, "코호스트"));

            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getCode()).isEqualTo("LIVE400_3");
            assertThat(response.getMessage()).isEqualTo(StreamErrorCode.INVALID_CO_HOST.getMessage());
            assertThat(response.getResult()).isEqualTo(new Payload(3L, "코호스트"));
        }

        @Test
        @DisplayName("of(code, data, message)는 데이터와 메시지를 모두 지정한다")
        void ofWithDataAndMessage() {
            ErrorResponse<List<String>> response = ErrorResponse.of(
                    GeneralErrorCode.BAD_REQUEST_ERROR, List.of("title", "genre"), "검증에 실패한 필드가 있습니다."
            );

            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getCode()).isEqualTo("COMMON_400_1");
            assertThat(response.getMessage()).isEqualTo("검증에 실패한 필드가 있습니다.");
            assertThat(response.getResult()).containsExactly("title", "genre");
        }

        @Test
        @DisplayName("빌더로도 동일한 응답을 만든다")
        void builder() {
            ErrorResponse<String> response = ErrorResponse.<String>builder()
                    .result("detail")
                    .baseResponseCode(GeneralErrorCode.SERVER_ERROR)
                    .build();

            assertThat(response.getIsSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getResult()).isEqualTo("detail");
        }

        @Test
        @DisplayName("message를 null로 덮어써도 NPE 없이 직렬화된다")
        void allowsNullOverriddenMessage() throws Exception {
            ErrorResponse<Void> response = ErrorResponse.<Void>of(GeneralErrorCode.BAD_REQUEST_ERROR, (String) null);

            assertThat(response.getMessage()).isNull();

            JsonNode json = serialize(response);
            assertThat(json.get("message").isNull()).isTrue();
            assertThat(json.get("code").asText()).isEqualTo("COMMON_400_1");
        }

        @Test
        @DisplayName("JSON 필드는 isSuccess/status/code/message/result/timeStamp 여섯 개다")
        void pinsJsonFieldNames() throws Exception {
            JsonNode json = serialize(ErrorResponse.from(StreamErrorCode.ALREADY_LIVE));

            assertThat(fieldNamesOf(json))
                    .containsExactlyInAnyOrder("isSuccess", "status", "code", "message", "result", "timeStamp");
            assertThat(json.get("isSuccess").asBoolean()).isFalse();
            assertThat(json.get("status").asInt()).isEqualTo(409);
            assertThat(json.get("code").asText()).isEqualTo("LIVE409_1");
            assertThat(json.get("message").asText()).isEqualTo(StreamErrorCode.ALREADY_LIVE.getMessage());
            assertThat(json.get("result").isNull()).isTrue();
            assertThat(json.get("timeStamp").asText()).matches(TIMESTAMP_PATTERN);
        }

        @Test
        @DisplayName("@JsonPropertyOrder대로 isSuccess, status, code, message, result, timeStamp 순서로 직렬화된다")
        void pinsJsonFieldOrder() throws Exception {
            String json = objectMapper.writeValueAsString(ErrorResponse.from(GeneralErrorCode.SERVER_ERROR));

            assertThat(json).containsSubsequence(
                    "\"isSuccess\"", "\"status\"", "\"code\"", "\"message\"", "\"result\"", "\"timeStamp\""
            );
        }

        @ParameterizedTest
        @EnumSource(GeneralErrorCode.class)
        @DisplayName("모든 공통 에러 코드가 status/code/message로 그대로 매핑된다")
        void mapsEveryGeneralErrorCode(GeneralErrorCode code) {
            ErrorResponse<Void> response = ErrorResponse.from(code);

            assertThat(response.getIsSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo(code.getStatus());
            assertThat(response.getCode()).isEqualTo(code.getCode());
            assertThat(response.getMessage()).isEqualTo(code.getMessage());
        }
    }

    @Nested
    @DisplayName("Jackson 왕복")
    class JacksonRoundTrip {

        @Test
        @DisplayName("직렬화 결과를 Map으로 다시 읽으면 필드 이름과 값이 보존된다")
        void roundTripsThroughMap() throws Exception {
            ErrorResponse<Payload> original =
                    ErrorResponse.of(StreamErrorCode.REPORT_TARGET_NOT_FOUND, new Payload(9L, "대상"));

            String json = objectMapper.writeValueAsString(original);
            Map<String, Object> decoded = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });

            assertThat(decoded)
                    .containsEntry("isSuccess", false)
                    .containsEntry("status", 404)
                    .containsEntry("code", "LIVE404_5")
                    .containsEntry("message", StreamErrorCode.REPORT_TARGET_NOT_FOUND.getMessage())
                    .containsKey("timeStamp");
            assertThat(decoded.get("result")).isEqualTo(Map.of("id", 9, "name", "대상"));
        }

        @Test
        @DisplayName("엔벨로프는 응답 전용이라 기본 생성자가 없어 역직렬화되지 않는다")
        void isSerializeOnly() {
            String json = """
                    {"isSuccess":false,"status":500,"code":"COMMON_500_1","message":"에러","result":null,"timeStamp":"2026-07-26 00:00:00"}
                    """;

            assertThatThrownBy(() -> objectMapper.readValue(json, ApiResponse.class))
                    .isInstanceOf(JsonMappingException.class);
        }
    }
}

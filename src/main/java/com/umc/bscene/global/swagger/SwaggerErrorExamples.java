package com.umc.bscene.global.swagger;

import com.umc.bscene.domain.auth.enums.code.OnboardingErrorCode;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.chat.enums.code.error.ChatErrorCode;
import com.umc.bscene.domain.follow.response.code.FollowErrorCode;
import com.umc.bscene.domain.notification.response.code.NotificationErrorCode;
import com.umc.bscene.domain.performance.response.code.PerformanceErrorCode;
import com.umc.bscene.domain.post.response.code.PostErrorCode;
import com.umc.bscene.domain.search.response.code.SearchErrorCode;
import com.umc.bscene.domain.session.enums.code.error.SessionErrorCode;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.user.response.code.UserErrorCode;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.code.BaseResponseCode;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SwaggerErrorExamples {

    private static final String EXAMPLE_REF = "#/components/examples/";

    private SwaggerErrorExamples() {
    }

    public static void register(OpenAPI document) {
        Set<String> referenced = document.getPaths().values().stream()
                .flatMap(path -> path.readOperations().stream())
                .flatMap(operation -> operation.getResponses().values().stream())
                .map(ApiResponse::getContent)
                .filter(Objects::nonNull)
                .flatMap(content -> content.values().stream())
                .map(MediaType::getExamples)
                .filter(Objects::nonNull)
                .flatMap(examples -> examples.values().stream())
                .map(Example::get$ref)
                .filter(ref -> ref != null && ref.startsWith(EXAMPLE_REF))
                .map(ref -> ref.substring(EXAMPLE_REF.length()))
                .collect(Collectors.toSet());
        referenced.add(GeneralErrorCode.UNAUTHORIZED_ERROR.getCode());

        Map<String, BaseResponseCode> errorCodes = Stream.<BaseResponseCode[]>of(
                        GeneralErrorCode.values(), OnboardingErrorCode.values(), UserErrorCode.values(),
                        SessionErrorCode.values(), BandErrorCode.values(),
                        PostErrorCode.values(), PerformanceErrorCode.values(), ChatErrorCode.values(),
                        FollowErrorCode.values(), SearchErrorCode.values(), StreamErrorCode.values(),
                        NotificationErrorCode.values())
                .flatMap(Arrays::stream)
                .filter(code -> referenced.contains(code.getCode()))
                .collect(Collectors.toMap(BaseResponseCode::getCode, Function.identity()));
        // 멤버 초대 오류와 코드 값이 같지만 메시지가 다르므로 예시 이름을 구분한다.
        errorCodes.put("PROFILE_ACTIVATION_FORBIDDEN", BandErrorCode.PROFILE_ACTIVATION_FORBIDDEN);
        errorCodes.put("BAND_MEMBER_INVITE_FORBIDDEN", BandErrorCode.BAND_MEMBER_INVITE_FORBIDDEN);
        // 라이브 채팅에도 LIVE404_6이 있으므로 공동 진행 초대의 응답을 별도 이름으로 등록한다.
        errorCodes.put("CO_HOST_INVITATION_NOT_FOUND", StreamErrorCode.CO_HOST_INVITATION_NOT_FOUND);
        errorCodes.put("LIVE_CHAT_NOT_OPEN", ChatErrorCode.LIVE_NOT_OPEN);

        referenced.stream().sorted().forEach(code -> {
            BaseResponseCode errorCode = Objects.requireNonNull(errorCodes.get(code), code);
            document.getComponents().addExamples(code, new Example().value(ErrorResponse.from(errorCode)));
        });
        document.getComponents().addResponses("AuthenticationRequired", new ApiResponse()
                .description("인증 실패")
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                        .addExamples("COMMON_401_1", new Example().$ref(EXAMPLE_REF + "COMMON_401_1")))));
    }
}

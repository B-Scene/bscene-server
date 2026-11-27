package com.umc.bscene.global.config;

import com.umc.bscene.global.swagger.SwaggerErrorExamples;
import com.umc.bscene.global.swagger.WebSocketOpenApi;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerOpenApiConfig {

    private static final String AUTH = "인증/인가";
    private static final String ONBOARDING = "온보딩";
    private static final String MY = "마이";
    private static final String BAND_MEMBER_PROFILE = "밴드 멤버 프로필";
    private static final String BAND_PROFILE = "밴드모드_내밴드(밴드 프로필)";
    private static final String BAND_POST = "밴드모드_내밴드(게시물)";
    private static final String BAND_PERFORMANCE = "밴드모드_내밴드(공연)";
    private static final String BAND_MUSIC_LINK = "밴드모드_내밴드(음원 링크)";
    private static final String SESSION = "세션";
    private static final String FAN_HOME = "팬모드_홈";
    private static final String FAN_EXPLORE = "팬모드_탐색";
    private static final String FAN_LIVE = "팬모드_라이브";
    private static final String BAND_LIVE = "밴드모드_라이브";
    private static final String MEDIA = "미디어 업로드 (공통)";
    private static final String NOTIFICATION = "알림";
    private static final String MOBILE_SDK = "모바일 SDK";
    private static final List<String> DOCUMENTED_TAGS = List.of(
            AUTH, ONBOARDING, MY, BAND_MEMBER_PROFILE, BAND_PROFILE, BAND_POST, BAND_PERFORMANCE, BAND_MUSIC_LINK,
            SESSION, FAN_HOME, FAN_EXPLORE, FAN_LIVE, BAND_LIVE, MEDIA, NOTIFICATION, MOBILE_SDK, WebSocketOpenApi.TAG);
    private static final String KAKAO_START = "/oauth2/authorization/kakao";
    private static final String KAKAO_CALLBACK = "/oauth/callback/kakao";
    private static final List<String> PATH_ORDER = List.of(
            "/auth/login",
            "/auth/logout",
            KAKAO_START,
            KAKAO_CALLBACK,
            "/auth/oauth/exchange",
            "/auth/oauth/signup",
            "/auth/reissue",
            "/users/me/onboarding/status",
            "/onboarding/fan-nickname/check",
            "/onboarding",
            "/genres",
            "/regions",
            "/users/me/profiles",
            "/users/me/mode",
            "/users/me",
            "/users/me/performance/history",
            "/users/me/performance/interest",
            "/users/me/follows",
            "/users/me/information",
            "/users/me/recruitments/receives",
            "/users/me/{applySubmissionId}/acceptance",
            "/users/me/{applySubmissionId}/final",
            "/sessions/recruitments/submissions/{applicationSubmissionId}",
            "/sessions/recruitments/manage",
            "/sessions/recruitments/{sessionRecruitmentId}/edit",
            "/sessions/recruitments",
            "/sessions/recruitments/{sessionRecruitmentId}",
            "/bands/{bandId}/leave",
            "/bands/{bandId}/owner",
            "/band-member-profiles",
            "/band-member-profiles/active",
            "/band-member-profiles/{profileId}",
            "/band-member-profiles/{profileId}/activate",
            "/bands",
            "/bands/check-name",
            "/bands/{bandId}",
            "/bands/{bandId}/members",
            "/bands/{bandId}/members/accept",
            "/bands/{bandId}/members/reject",
            "/bands/{bandId}/members/{userId}",
            "/bands/{bandId}/members/search",
            "/bands/{bandId}/invite-links",
            "/band-invite-links/{token}",
            "/band-invite-links/{token}/entries",
            "/bands/follower",
            "/bands/{bandId}/posts",
            "/posts/{postId}",
            "/bands/{bandId}/performances",
            "/performances/{performanceId}",
            "/bands/{bandId}/music-links",
            "/sessions/recruitments/{sessionRecruitmentId}/interest",
            "/sessions/recruitments/search-history",
            "/sessions/recruitments/search-history/{keywordId}",
            "/sessions/applications/search",
            "/sessions/applications/{sessionApplicationId}",
            "/sessions/applications/{sessionApplicationId}/visibility",
            "/sessions/applications/summary",
            "/sessions/profile",
            "/sessions/profile/name",
            "/sessions/profile/image",
            "/sessions/applications",
            "/sessions/applications/my/{sessionApplicationId}",
            "/sessions/recruitments/{sessionRecruitmentId}/applications",
            "/sessions/applications/submissions/{applicationSubmissionId}",
            "/sessions/applications/submissions",
            "/sessions/recruitments/interests",
            "/sessions/recruitments/recently-viewed",
            "/chat/rooms",
            "/chat/rooms/{chatRoomId}",
            "/home",
            "/performances/participation/pending",
            "/performances/{performanceId}/alarm",
            "/performances/{performanceId}/interest",
            "/performances/{performanceId}/participation/complete",
            "/performances/{performanceId}/participation",
            "/performances/{performanceId}/detail",
            "/posts/following",
            "/performances/upcoming",
            "/performances/recommend",
            "/performances/calendar",
            "/performances/by-date",
            "/bands/{bandId}/members/profiles",
            "/bands/recommendations",
            "/explore/search",
            "/bands/{bandId}/follow",
            "/explore/search/recent",
            "/explore/search/recent/{recentSearchId}",
            "/bands/{bandId}/detail",
            "/posts/{postId}/detail",
            "/posts/{postId}/comments",
            "/posts/{postId}/comments/{commentId}",
            "/posts/{postId}/likes",
            "/lives/home",
            "/lives",
            "/lives/{liveId}",
            "/lives/{liveId}/co-host",
            "/lives/{liveId}/co-host/acceptance",
            "/lives/{liveId}/viewers",
            "/lives/{liveId}/members",
            "/lives/{liveId}/close",
            "/lives/{liveId}/summary",
            "/lives/{liveId}/leave",
            "/lives/{liveId}/reports",
            "/users/blocks",
            "/lives/live-now/following",
            "/lives/live-now/all",
            "/lives/scheduled",
            "/lives/{liveId}/alarm",
            "/lives/replays/following",
            "/lives/replays/all",
            "/lives/{liveId}/replay",
            "/lives/{liveId}/replay/playlist",
            "/lives/{liveId}/reservation",
            "/lives/{liveId}/co-host-invitation",
            "/media/presigned-url",
            "/notifications/tokens",
            "/notifications/test",
            "/notifications/{notificationId}/read",
            "/users/me/notification-settings",
            "/users/me/notification-settings/{settingType}",
            "/notifications",
            "/auth/oauth/mobile/kakao",
            "/auth/oauth/mobile/apple",
            "/auth/oauth/mobile/apple/challenge",
            "/lives/{liveId}/chat/ws-ticket",
            "/ws/lives/{liveId}/chat",
            "/chat/ws-ticket",
            "/ws/chat"
    );

    @Bean
    OpenAPI bsceneOpenApi(@Value("${server.servlet.context-path:}") String contextPath) {
        return new OpenAPI()
                .info(new Info().title("B-Scene API").version("v1").description("""
                        ### 밴드 역할별 API 권한

                        | 호출 권한 | 기능 및 조건 |
                        | --- | --- |
                        | 오너 | 밴드 프로필 수정, 멤버·세션 초대·내보내기, 초대 링크 발급, 오너 권한 양도, 세션 모집 공고 등록·수정·삭제 |
                        | 오너 또는 가입이 완료된 멤버·세션 | 소속 밴드 게시물과 공연 일정의 등록·수정·삭제, 음원 링크 저장 |
                        | 오너 또는 가입이 완료된 정식 멤버(MEMBER) | 밴드 모드에서 활성 밴드 프로필로 라이브 생성 및 예약 관리. 공동 진행 초대 후보는 해당 밴드의 가입 완료 정식 멤버 |
                        | 해당 라이브의 송출자 또는 승인된 공동 진행자 | 밴드 모드에서 라이브 입장. 공동 진행자는 초대 수락 또는 공동 송출 요청 승인 필요 |

                        오너는 초대 때 멤버 또는 세션 역할을 지정합니다. 오너 권한은 가입이 완료된 정식 멤버에게 양도할 수 있습니다.
                        API 호출에는 서비스 액세스 토큰을 사용합니다.
                        """))
                .addServersItem(new Server().url(contextPath.isBlank() ? "/" : contextPath))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addTagsItem(new Tag().name(AUTH).description("로그인, 회원가입, 토큰 재발급 및 로그아웃"))
                .addTagsItem(new Tag().name(ONBOARDING).description("온보딩 상태, 닉네임, 장르 및 지역"))
                .addTagsItem(new Tag().name(MY).description("내 프로필, 팬·밴드 마이페이지 및 내 밴드 관리"))
                .addTagsItem(new Tag().name(BAND_MEMBER_PROFILE).description("내 밴드 멤버 프로필 관리"))
                .addTagsItem(new Tag().name(BAND_PROFILE).description("밴드 프로필, 멤버 초대, 초대 링크 및 팔로워"))
                .addTagsItem(new Tag().name(BAND_POST).description("밴드 게시물 작성, 조회, 수정 및 삭제"))
                .addTagsItem(new Tag().name(BAND_PERFORMANCE).description("밴드 공연 등록, 조회, 수정 및 삭제"))
                .addTagsItem(new Tag().name(BAND_MUSIC_LINK).description("밴드 음원 링크 조회 및 저장"))
                .addTagsItem(new Tag().name(SESSION).description("세션 프로필, 지원서, 모집 공고, 세션 찾기 및 쪽지"))
                .addTagsItem(new Tag().name(FAN_HOME).description("팬 홈, 팔로우 밴드 소식, 공연 및 참여 확인"))
                .addTagsItem(new Tag().name(FAN_EXPLORE).description("추천 밴드, 통합 검색, 최근 검색어 및 게시물"))
                .addTagsItem(new Tag().name(FAN_LIVE).description("라이브 홈, 오디오 청취, 시청자 수, 예약 알림 및 다시보기"))
                .addTagsItem(new Tag().name(BAND_LIVE).description("라이브 생성, 오디오 송출, 공동 진행, 예약 및 다시보기 등록"))
                .addTagsItem(new Tag().name(MEDIA).description("S3 직접 업로드 URL 발급"))
                .addTagsItem(new Tag().name(NOTIFICATION).description("푸시 토큰, 알림 목록, 읽음 처리 및 알림 설정"))
                .addTagsItem(new Tag().name(MOBILE_SDK).description("카카오 SDK 로그인과 Apple SDK 로그인. Apple SDK 호출 전에 로그인 요청을 발급합니다."))
                .addTagsItem(new Tag().name(WebSocketOpenApi.TAG).description("라이브 채팅·쪽지 연결 티켓 및 실시간 메시지"));
    }

    @Bean
    OpenApiCustomizer documentedEndpoints() {
        return openApi -> {
            // 이 두 경로는 Spring Security 필터가 처리해 컨트롤러 자동 탐색에 나타나지 않는다.
            Paths detected = openApi.getPaths();
            detected.addPathItem(KAKAO_START, new PathItem().get(
                    redirect("startKakaoWebLogin", "카카오 소셜 로그인 시작",
                            "웹 브라우저를 카카오 로그인 화면으로 이동시킵니다. "
                                    + "허용된 redirect_origin이 전달되면 로그인 후 해당 프론트 주소로 돌아갑니다.",
                            "카카오 로그인 화면 URL")
                            .addParametersItem(query("redirect_origin", "로그인 완료 후 돌아갈 프론트 Origin"))));
            detected.addPathItem(KAKAO_CALLBACK, new PathItem().get(
                    redirect("handleKakaoWebCallback", "카카오 OAuth 콜백 처리",
                            "카카오가 호출합니다. 성공하면 프론트 콜백으로 일회용 code를 전달하고, "
                                    + "실패하면 프론트 로그인 페이지로 error를 전달합니다.",
                            "성공: /oauth/callback?code=… / 실패: /login?error=…")
                            .addParametersItem(query("code", "성공 시 카카오가 보내는 인가 코드"))
                            .addParametersItem(query("state", "로그인 시작 요청과 연결하는 카카오 상태 값"))
                            .addParametersItem(query("error", "인가 실패 시 카카오가 보내는 오류 코드"))));

            WebSocketOpenApi.addHandshakes(detected);
            Paths ordered = new Paths();
            PATH_ORDER.forEach(path -> {
                PathItem discovered = detected.get(path);
                if (discovered == null) {
                    return;
                }
                PathItem documented = new PathItem();
                discovered.readOperationsMap().forEach((method, operation) -> {
                    if (operation.getTags() != null && operation.getTags().stream().anyMatch(DOCUMENTED_TAGS::contains)) {
                        documented.operation(method, operation);
                    }
                });
                if (!documented.readOperations().isEmpty()) {
                    ordered.addPathItem(path, documented);
                }
            });
            openApi.setPaths(ordered);
            SwaggerErrorExamples.register(openApi);
        };
    }

    private Operation redirect(String id, String summary, String description, String locationDescription) {
        return new Operation().operationId(id).addTagsItem(AUTH)
                .summary(summary).description(description)
                .responses(new ApiResponses().addApiResponse("302",
                        new ApiResponse().description("Found (응답 본문 없음)")
                                .addHeaderObject("Location", new Header()
                                        .description(locationDescription).schema(new StringSchema()))));
    }

    private QueryParameter query(String name, String description) {
        QueryParameter parameter = new QueryParameter();
        parameter.setName(name);
        parameter.setDescription(description);
        parameter.setSchema(new StringSchema());
        return parameter;
    }
}

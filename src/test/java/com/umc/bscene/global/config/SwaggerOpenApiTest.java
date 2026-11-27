package com.umc.bscene.global.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.notification.entity.Notification;
import com.umc.bscene.domain.notification.repository.NotificationRepository;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.StreamReplay;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.StreamReplayRepository;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.enums.UserRole;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.notification.enums.NotificationType;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "swagger.admin.token=test-swagger-credential")
@ActiveProfiles("qa")
@Transactional
class SwaggerOpenApiTest {

    private static final String DOCS = "/v3/api-docs";

    @Autowired private WebApplicationContext context;
    @Autowired private Validator validator;
    @Autowired private UserRepository users;
    @Autowired private BandRepository bands;
    @Autowired private BandMemberRepository bandMembers;
    @Autowired private BandMemberProfileRepository bandMemberProfiles;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private AudioStreamRepository audioStreams;
    @Autowired private StreamReplayRepository streamReplays;
    @Autowired private NotificationRepository notifications;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void controllerValidationIsCompatibleWithSwaggerInterfaces() {
        var mappings = context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        mappings.getHandlerMethods().values().stream()
                .map(handler -> handler.getBeanType())
                .filter(type -> type.getName().startsWith("com.umc.bscene."))
                .distinct()
                .forEach(type -> assertThatCode(() -> validator.getConstraintsForClass(type))
                        .as(type.getSimpleName()).doesNotThrowAnyException());
    }

    @Test
    void protectsAllSwaggerResourcesAndAcceptsAdminOnly() throws Exception {
        for (String path : List.of("/swagger-ui.html", "/swagger-ui/index.html",
                "/swagger-ui/swagger-ui-bundle.js", DOCS, DOCS + ".yaml", DOCS + "/swagger-config")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }

        mvc.perform(get(DOCS).header(AUTHORIZATION, bearer(UserRole.USER)))
                .andExpect(status().isForbidden());
        mvc.perform(get(DOCS).header(AUTHORIZATION, bearer(UserRole.ADMIN)))
                .andExpect(status().isOk());
        mvc.perform(get(DOCS).with(httpBasic("bscene-admin", "invalid")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(DOCS).with(httpBasic("bscene-admin", "test-swagger-credential"))
                        .cookie(new Cookie("access_token", "expired-browser-cookie")))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerCredentialIsNotAnApplicationCredentialAndPublicAuthStillWorks() throws Exception {
        mvc.perform(get("/users/me").with(httpBasic("bscene-admin", "test-swagger-credential")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/reissue").contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"invalid\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("AUTH401_4"));
    }

    @Test
    void generatesOrderedAuthOperationsWithTypedSchemasAndObjectExamples() throws Exception {
        JsonNode document = swaggerDocument();
        Map<String, JsonNode> paths = new LinkedHashMap<>();
        document.path("paths").fields().forEachRemaining(entry -> {
            if (entry.getValue().elements().hasNext()
                    && entry.getValue().elements().next().at("/tags/0").asText().equals("인증/인가")) {
                paths.put(entry.getKey(), entry.getValue());
            }
        });
        assertThat(paths.keySet()).containsExactly(
                "/auth/login", "/auth/logout", "/oauth2/authorization/kakao", "/oauth/callback/kakao",
                "/auth/oauth/exchange", "/auth/oauth/signup", "/auth/reissue");
        assertThat(document.at("/tags/0/name").asText()).isEqualTo("인증/인가");
        assertThat(document.at("/servers/0/url").asText()).isEqualTo("/api");

        paths.forEach((path, item) -> {
            JsonNode operation = item.path("post");
            if (operation.isMissingNode()) {
                assertThat(item.at("/get/responses/302/headers/Location")).isNotEmpty();
                assertThat(item.at("/get/responses/302/content").isMissingNode()).isTrue();
                return;
            }
            JsonNode request = operation.at("/requestBody/content/application~1json");
            assertObjectSchema(document, request.path("schema"), path + " request");
            assertObjectExamples(request);
            operation.path("responses").fields().forEachRemaining(response -> {
                JsonNode json = response.getValue().at("/content/application~1json");
                assertObjectSchema(document, json.path("schema"), path + " " + response.getKey());
                assertObjectExamples(json);
            });
        });

        JsonNode signup = paths.get("/auth/oauth/signup").path("post");
        JsonNode signupSchema = resolve(document, signup.at("/requestBody/content/application~1json/schema"));
        assertThat(signupSchema.at("/properties/terms/type").asText()).isEqualTo("array");
        assertThat(signupSchema.at("/properties/phone").isMissingNode()).isTrue();
        JsonNode signupExample = requestExample(document.path("paths"), "/auth/oauth/signup");
        assertThat(signupExample.has("phone")).isFalse();
        assertThat(signupExample.path("terms").size()).isEqualTo(4);
        assertThat(signupExample.at("/terms/3/termId").asInt()).isEqualTo(4);
        assertThat(signupExample.at("/terms/3/agreed").isBoolean()).isTrue();
        assertThat(signupExample.at("/terms/3/agreed").asBoolean()).isFalse();
        assertThat(requestExample(document.path("paths"), "/auth/login").path("loginId").asText())
                .isEqualTo("bscene123@example.com");
        assertThat(requestExample(document.path("paths"), "/auth/logout"))
                .isEqualTo(requestExample(document.path("paths"), "/auth/reissue"));
        assertThat(requestExample(document.path("paths"), "/auth/reissue").path("refreshToken").asText())
                .isEqualTo("refresh-token");
        JsonNode tokenEnvelope = resolve(document, signup.at("/responses/201/content/application~1json/schema"));
        JsonNode token = resolve(document, tokenEnvelope.at("/properties/result"));
        assertThat(token.at("/properties/accessTokenExpiresIn/type").asText()).isEqualTo("integer");
        assertThat(resolve(document, token.at("/properties/user")).at("/properties/onboardingCompleted/type")
                .asText()).isEqualTo("boolean");
    }

    @Test
    void listsOnboardingAndMyEndpointsWithTypedBodiesInNotionOrder() throws Exception {
        JsonNode document = swaggerDocument();
        JsonNode paths = document.path("paths");
        assertThat(document.path("tags").elements()).toIterable()
                .extracting(tag -> tag.path("name").asText()).containsExactly(
                        "인증/인가", "온보딩", "마이", "밴드 멤버 프로필", "밴드모드_내밴드(밴드 프로필)",
                        "밴드모드_내밴드(게시물)", "밴드모드_내밴드(공연)", "밴드모드_내밴드(음원 링크)",
                        "세션", "팬모드_홈", "팬모드_탐색", "팬모드_라이브", "밴드모드_라이브",
                        "미디어 업로드 (공통)", "알림", "모바일 SDK", "웹소켓");
        List<String> orderedPaths = new ArrayList<>();
        paths.fields().forEachRemaining(entry -> {
            entry.getValue().fields().forEachRemaining(method -> {
                JsonNode operation = method.getValue();
                if (!List.of("온보딩", "마이").contains(operation.at("/tags/0").asText())) {
                    return;
                }
                if (!orderedPaths.contains(entry.getKey())) {
                    orderedPaths.add(entry.getKey());
                }
                String label = method.getKey() + " " + entry.getKey();
                if (operation.has("requestBody")) {
                    JsonNode body = operation.at("/requestBody/content/application~1json");
                    assertObjectSchema(document, body.path("schema"), label);
                    assertObjectExamples(body);
                }
                String status = entry.getKey().endsWith("/final") ? "201" : "200";
                JsonNode response = operation.path("responses").path(status).at("/content/application~1json");
                assertObjectSchema(document, response.path("schema"), label);
                assertObjectExamples(response);
            });
        });
        assertThat(orderedPaths).containsExactly("/users/me/onboarding/status", "/onboarding/fan-nickname/check",
                "/onboarding", "/genres", "/regions", "/users/me/profiles", "/users/me/mode", "/users/me",
                "/users/me/performance/history", "/users/me/performance/interest", "/users/me/follows",
                "/users/me/information", "/users/me/recruitments/receives", "/users/me/{applySubmissionId}/acceptance",
                "/users/me/{applySubmissionId}/final", "/sessions/recruitments/submissions/{applicationSubmissionId}",
                "/sessions/recruitments/manage", "/sessions/recruitments/{sessionRecruitmentId}/edit",
                "/sessions/recruitments/{sessionRecruitmentId}", "/bands/{bandId}/leave", "/bands/{bandId}/owner");

        JsonNode recruitment = paths.path("/sessions/recruitments/{sessionRecruitmentId}");
        assertThat(recruitment.at("/patch/tags/0").asText()).isEqualTo("마이");
        assertThat(recruitment.at("/delete/tags/0").asText()).isEqualTo("마이");
        assertThat(paths.path("/users/me/mode").fieldNames()).toIterable().containsExactlyInAnyOrder("put", "patch");
        assertThat(paths.path("/users/me/information").fieldNames()).toIterable().containsExactlyInAnyOrder("get", "patch");
        assertThat(paths.path("/users/me").at("/get/security/0/bearerAuth").isArray()).isTrue();
        assertThat(paths.path("/genres").at("/get/security").isMissingNode()).isTrue();
        assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");

        JsonNode result = resolve(document, paths.path("/users/me")
                .at("/get/responses/200/content/application~1json/schema")).at("/properties/result");
        assertThat(result.path("oneOf").size()).isEqualTo(2);
        paths.path("/users/me/follows").at("/get/responses/200/content/application~1json/examples")
                .elements().forEachRemaining(example -> example.at("/value/result/items").forEach(item ->
                        assertThat(Genre.valueOf(item.path("genre").asText())).isNotNull()));
    }

    @Test
    void listsBandEndpointsWithTypedPayloadsInDocumentationOrder() throws Exception {
        JsonNode document = swaggerDocument();
        JsonNode paths = document.path("paths");
        List<String> orderedPaths = new ArrayList<>();
        int operationCount = 0;
        var pathEntries = paths.fields();
        while (pathEntries.hasNext()) {
            var entry = pathEntries.next();
            var operations = entry.getValue().fields();
            boolean bandPath = false;
            while (operations.hasNext()) {
                var method = operations.next();
                JsonNode operation = method.getValue();
                if (!List.of("밴드 멤버 프로필", "밴드모드_내밴드(밴드 프로필)")
                        .contains(operation.at("/tags/0").asText())) {
                    continue;
                }
                operationCount++;
                bandPath = true;
                String label = method.getKey() + " " + entry.getKey();
                if (operation.has("requestBody")) {
                    JsonNode request = operation.at("/requestBody/content/application~1json");
                    assertObjectSchema(document, request.path("schema"), label);
                    assertObjectExamples(request);
                }
                JsonNode response = operation.at("/responses/201/content/application~1json");
                if (response.isMissingNode()) {
                    response = operation.at("/responses/200/content/application~1json");
                }
                assertObjectSchema(document, response.path("schema"), label);
                assertObjectExamples(response);
            }
            if (bandPath) {
                orderedPaths.add(entry.getKey());
            }
        }
        assertThat(operationCount).isEqualTo(21);
        assertThat(orderedPaths).containsExactly(
                "/band-member-profiles", "/band-member-profiles/active", "/band-member-profiles/{profileId}",
                "/band-member-profiles/{profileId}/activate", "/bands", "/bands/check-name", "/bands/{bandId}",
                "/bands/{bandId}/members", "/bands/{bandId}/members/accept", "/bands/{bandId}/members/reject",
                "/bands/{bandId}/members/{userId}", "/bands/{bandId}/members/search",
                "/bands/{bandId}/invite-links", "/band-invite-links/{token}",
                "/band-invite-links/{token}/entries", "/bands/follower");
        assertThat(paths.path("/band-invite-links/{token}").at("/get/security").isMissingNode()).isTrue();
        assertThat(paths.path("/band-invite-links/{token}/entries").at("/post/security/0/bearerAuth").isArray()).isTrue();
        assertThat(paths.has("/bands/{bandId}/members/{memberId}")).isFalse();
        assertThat(paths.path("/bands/{bandId}/members/{userId}").at("/delete/parameters/1/name").asText())
                .isEqualTo("userId");
    }

    @Test
    void bandSwaggerExamplesMatchActualProfileCreationAndInviteResponses() throws Exception {
        JsonNode paths = swaggerDocument().path("paths");
        String owner = bearer(UserRole.USER);
        JsonNode memberProfile = compareBandResponse(paths, "/band-member-profiles", "post",
                mvc.perform(post("/band-member-profiles").header(AUTHORIZATION, owner)
                                .contentType(APPLICATION_JSON)
                                .content(requestExample(paths, "/band-member-profiles").toString()))
                        .andExpect(status().isCreated()), "id", "createdAt");

        ObjectNode createBand = requestExample(paths, "/bands").deepCopy();
        createBand.put("bandMemberProfileId", memberProfile.path("id").asLong());
        JsonNode band = compareBandResponse(paths, "/bands", "post",
                mvc.perform(post("/bands").header(AUTHORIZATION, owner)
                                .contentType(APPLICATION_JSON).content(createBand.toString()))
                        .andExpect(status().isCreated()), "id", "ownerId");
        long bandId = band.path("id").asLong();
        mvc.perform(get("/bands/check-name").param("name", createBand.path("name").asText())
                        .header(AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.available").value(false));

        String invitePath = "/bands/{bandId}/invite-links";
        JsonNode invite = compareBandResponse(paths, invitePath, "post",
                mvc.perform(post(invitePath, bandId).header(AUTHORIZATION, owner)
                                .contentType(APPLICATION_JSON).content(requestExample(paths, invitePath).toString()))
                        .andExpect(status().isCreated()), "bandId", "token", "expiresAt");
        String inviteToken = invite.path("token").asText();
        mvc.perform(get("/band-invite-links/{token}", inviteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BAND200_13"))
                .andExpect(jsonPath("$.result.bandId").value(bandId))
                .andExpect(jsonPath("$.result.memberType").value("SESSION"));
        String entryPath = "/band-invite-links/{token}/entries";
        compareBandResponse(paths, entryPath, "post",
                mvc.perform(post(entryPath, inviteToken).header(AUTHORIZATION, bearer(UserRole.USER)))
                        .andExpect(status().isCreated()), "bandMemberId", "bandId");
    }

    @Test
    void documentsOnlyTheSpecifiedBandContentOperationsWithTypedBodies() throws Exception {
        JsonNode document = swaggerDocument();
        JsonNode paths = document.path("paths");
        Map<String, List<String>> methods = new LinkedHashMap<>();
        methods.put("/bands/{bandId}/posts", List.of("post", "get"));
        methods.put("/posts/{postId}", List.of("get", "put", "delete"));
        methods.put("/bands/{bandId}/performances", List.of("post", "get"));
        methods.put("/performances/{performanceId}", List.of("get", "patch", "delete"));
        methods.put("/bands/{bandId}/music-links", List.of("get", "put"));
        List<String> orderedPaths = new ArrayList<>();
        paths.fieldNames().forEachRemaining(path -> {
            if (methods.containsKey(path)) {
                orderedPaths.add(path);
            }
        });
        assertThat(orderedPaths).containsExactlyElementsOf(methods.keySet());
        methods.forEach((path, verbs) -> {
            assertThat(paths.path(path).fieldNames()).toIterable().containsExactlyInAnyOrderElementsOf(verbs);
            for (String verb : verbs) {
                JsonNode operation = paths.path(path).path(verb);
                assertThat(operation.at("/security/0/bearerAuth").isArray()).isTrue();
                if (List.of("post", "put", "patch").contains(verb)) {
                    JsonNode request = operation.at("/requestBody/content/application~1json");
                    assertObjectSchema(document, request.path("schema"), verb + " " + path);
                    assertObjectExamples(request);
                }
                String status = verb.equals("post") && path.endsWith("/performances") ? "201" : "200";
                JsonNode response = operation.path("responses").path(status).at("/content/application~1json");
                assertObjectSchema(document, response.path("schema"), verb + " " + path);
                assertObjectExamples(response);
            }
        });
        assertThat(paths.path("/posts/{postId}/detail").at("/get/tags/0").asText()).isEqualTo("팬모드_탐색");
        assertThat(paths.path("/performances/{performanceId}/detail").at("/get/tags/0").asText()).isEqualTo("팬모드_홈");
        assertThat(mediaExample(paths.path("/performances/{performanceId}")
                .at("/patch/requestBody/content/application~1json")).path("ticketPrice").isTextual()).isTrue();
    }

    @Test
    void bandPostAndPerformanceExamplesMatchTheHttpApi() throws Exception {
        JsonNode paths = swaggerDocument().path("paths");
        Band band = createAcceptedBand();
        String token = "Bearer " + jwtUtil.createAccessToken(new AuthMember(band.getOwner()));
        String bandPosts = "/bands/{bandId}/posts";
        String postPath = "/posts/{postId}";
        JsonNode post = compareBandResponse(paths, bandPosts, "post",
                mvc.perform(post(bandPosts, band.getId()).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(requestExample(paths, bandPosts).toString()))
                        .andExpect(status().isOk()), "postId", "createdAt");
        long postId = post.path("postId").asLong();
        mvc.perform(get(bandPosts, band.getId()).header(AUTHORIZATION, token).param("type", "PHOTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.posts[0].postId").value(postId));
        compareBandResponse(paths, postPath, "get",
                mvc.perform(get(postPath, postId).header(AUTHORIZATION, token)).andExpect(status().isOk()),
                "postId", "bandId", "createdAt", "updatedAt");
        compareBandResponse(paths, postPath, "put",
                mvc.perform(put(postPath, postId).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(mediaExample(paths.path(postPath).at("/put/requestBody/content/application~1json")).toString()))
                        .andExpect(status().isOk()), "postId", "updatedAt");
        compareResponse(paths, postPath, "delete",
                mvc.perform(delete(postPath, postId).header(AUTHORIZATION, token)).andExpect(status().isOk()));

        String bandShows = "/bands/{bandId}/performances";
        String showPath = "/performances/{performanceId}";
        ObjectNode showRequest = requestExample(paths, bandShows).deepCopy();
        showRequest.put("performanceDate", LocalDate.now().plusDays(10).toString());
        JsonNode show = compareBandResponse(paths, bandShows, "post",
                mvc.perform(post(bandShows, band.getId()).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(showRequest.toString()))
                        .andExpect(status().isCreated()), "performanceId", "performanceDate");
        long showId = show.path("performanceId").asLong();
        mvc.perform(get(bandShows, band.getId()).header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.performances[0].performanceId").value(showId));
        compareBandResponse(paths, showPath, "get",
                mvc.perform(get(showPath, showId).header(AUTHORIZATION, token)).andExpect(status().isOk()),
                "performanceId", "performanceDate");
        compareBandResponse(paths, showPath, "patch",
                mvc.perform(patch(showPath, showId).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(mediaExample(paths.path(showPath).at("/patch/requestBody/content/application~1json")).toString()))
                        .andExpect(status().isOk()), "performanceId", "performanceDate");
        compareResponse(paths, showPath, "delete",
                mvc.perform(delete(showPath, showId).header(AUTHORIZATION, token)).andExpect(status().isOk()));
    }

    @Test
    void musicLinkSaveAndClearExamplesMatchTheHttpApi() throws Exception {
        JsonNode paths = swaggerDocument().path("paths");
        Band band = createAcceptedBand();
        String token = "Bearer " + jwtUtil.createAccessToken(new AuthMember(band.getOwner()));
        String path = "/bands/{bandId}/music-links";
        JsonNode requests = paths.path(path).at("/put/requestBody/content/application~1json/examples");
        compareResponse(paths, path, "put",
                mvc.perform(put(path, band.getId()).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(requests.at("/요청 예시/value").toString()))
                        .andExpect(status().isOk()));
        compareResponse(paths, path, "get",
                mvc.perform(get(path, band.getId()).header(AUTHORIZATION, token)).andExpect(status().isOk()));
        mvc.perform(put(path, band.getId()).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                        .content(requests.at("/링크 전체 삭제/value").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MUSIC200_2"))
                .andExpect(jsonPath("$.result.spotifyUrl").isEmpty())
                .andExpect(jsonPath("$.result.etcPlatform").isEmpty());
    }

    @Test
    void documentsAllSessionOperationsWithTypedJsonExamples() throws Exception {
        JsonNode document = swaggerDocument();
        JsonNode paths = document.path("paths");
        List<String> operations = new ArrayList<>();
        paths.fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(entry -> {
            JsonNode operation = entry.getValue();
            if (!operation.at("/tags/0").asText().equals("세션")) {
                return;
            }
            String location = entry.getKey() + " " + path.getKey();
            operations.add(location);
            assertThat(operation.at("/security/0").has("bearerAuth")).as(location).isTrue();
            assertThat(operation.at("/responses/401/$ref").asText()).as(location).endsWith("/AuthenticationRequired");
            JsonNode request = operation.at("/requestBody/content/application~1json");
            if (!request.isMissingNode()) {
                assertObjectSchema(document, request.path("schema"), location);
                assertObjectExamples(request);
            }
            JsonNode response = operation.path("responses")
                    .path(location.equals("post /sessions/applications") ? "201" : "200")
                    .at("/content/application~1json");
            assertObjectSchema(document, response.path("schema"), location);
            assertObjectExamples(response);
            assertThat(resolve(document, response.path("schema")).path("properties").has("result"))
                    .as(location).isTrue();
        }));
        assertThat(operations).containsExactlyInAnyOrder(
                "post /sessions/recruitments", "get /sessions/recruitments/{sessionRecruitmentId}",
                "get /sessions/recruitments", "post /sessions/recruitments/{sessionRecruitmentId}/interest",
                "delete /sessions/recruitments/{sessionRecruitmentId}/interest",
                "get /sessions/recruitments/search-history", "delete /sessions/recruitments/search-history/{keywordId}",
                "get /sessions/applications/search", "get /sessions/applications/{sessionApplicationId}",
                "patch /sessions/applications/{sessionApplicationId}/visibility", "get /sessions/applications/summary",
                "get /sessions/profile", "patch /sessions/profile/name", "patch /sessions/profile/image",
                "post /sessions/applications", "patch /sessions/applications/{sessionApplicationId}",
                "delete /sessions/applications/{sessionApplicationId}", "get /sessions/applications/my/{sessionApplicationId}",
                "post /sessions/recruitments/{sessionRecruitmentId}/applications",
                "delete /sessions/applications/submissions/{applicationSubmissionId}",
                "get /sessions/applications/submissions", "get /sessions/recruitments/interests",
                "get /sessions/recruitments/recently-viewed", "post /chat/rooms", "get /chat/rooms",
                "get /chat/rooms/{chatRoomId}", "delete /chat/rooms/{chatRoomId}");
        assertThat(paths.path("/sessions/profile").has("patch")).isFalse();
        JsonNode search = paths.path("/sessions/applications/search").path("get");
        assertThat(search.path("description").asText()).contains("기본 지원서", "활동 지역 및 장르");
        List<String> filters = new ArrayList<>();
        search.path("parameters").forEach(parameter -> filters.add(parameter.path("name").asText()));
        assertThat(filters).containsExactlyInAnyOrder("region", "skillLevel", "part", "genre", "keyword", "cursorId", "size");
    }

    @Test
    void sessionProfileSwaggerExamplesMatchActualHttpResponses() throws Exception {
        JsonNode document = swaggerDocument();
        JsonNode paths = document.path("paths");
        String token = bearer(UserRole.USER);
        String name = "/sessions/profile/name";
        String image = "/sessions/profile/image";
        compareResponse(paths, name, "patch",
                mvc.perform(patch(name).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(mediaExample(paths.path(name).at("/patch/requestBody/content/application~1json")).toString()))
                        .andExpect(status().isOk()));
        JsonNode imageRequests = paths.path(image).at("/patch/requestBody/content/application~1json/examples");
        compareResponse(paths, image, "patch",
                mvc.perform(patch(image).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(imageRequests.at("/사진 저장/value").toString()))
                        .andExpect(status().isOk()));
        mvc.perform(patch(image).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                        .content(imageRequests.at("/사진 삭제/value").toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.profileImageUrl").isEmpty());
        mvc.perform(patch(image).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                        .content("{\"profileImageUrl\":\"https://cdn.bscene.com/session/profile.gif\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SESSION400_11"))
                .andExpect(jsonPath("$.message").value(document.at("/components/examples/SESSION400_11/value/message").asText()));
    }

    @Test
    void sessionRecruitmentApplicationAndChatExamplesMatchActualHttpStatuses() throws Exception {
        JsonNode paths = swaggerDocument().path("paths");
        Band band = createAcceptedBand();
        BandMember member = bandMembers.findByBand_IdAndUser_Id(band.getId(), band.getOwner().getId()).orElseThrow();
        String owner = "Bearer " + jwtUtil.createAccessToken(new AuthMember(band.getOwner()));
        String applicant = bearer(UserRole.USER);
        String recruitments = "/sessions/recruitments";
        String applications = "/sessions/applications";
        ObjectNode recruitmentBody = (ObjectNode) requestExample(paths, recruitments).deepCopy();
        recruitmentBody.put("bandMemberId", member.getId());
        recruitmentBody.put("deadlineAt", LocalDateTime.now().plusDays(30).withNano(0).toString());
        JsonNode recruitment = compareBandResponse(paths, recruitments, "post",
                mvc.perform(post(recruitments).header(AUTHORIZATION, owner).contentType(APPLICATION_JSON)
                                .content(recruitmentBody.toString()))
                        .andExpect(status().isOk()), "sessionRecruitmentId", "bandId", "deadlineAt", "createdAt");
        long recruitmentId = recruitment.path("sessionRecruitmentId").asLong();

        JsonNode application = new ObjectMapper().readTree(mvc.perform(post(applications).header(AUTHORIZATION, applicant)
                        .contentType(APPLICATION_JSON).content(requestExample(paths, applications).toString()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("SESSION201_1"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        String submission = "/sessions/recruitments/{sessionRecruitmentId}/applications";
        ObjectNode submissionBody = (ObjectNode) requestExample(paths, submission).deepCopy();
        submissionBody.put("sessionApplicationId", application.at("/result/sessionApplicationId").asLong());
        compareBandResponse(paths, submission, "post",
                mvc.perform(post(submission, recruitmentId).header(AUTHORIZATION, applicant).contentType(APPLICATION_JSON)
                                .content(submissionBody.toString())).andExpect(status().isOk()),
                "applicationSubmissionId", "bandName");

        String rooms = "/chat/rooms";
        ObjectNode chatBody = (ObjectNode) requestExample(paths, rooms).deepCopy();
        chatBody.put("sessionRecruitmentId", recruitmentId);
        compareBandResponse(paths, rooms, "post",
                mvc.perform(post(rooms).header(AUTHORIZATION, applicant).contentType(APPLICATION_JSON)
                                .content(chatBody.toString())).andExpect(status().isOk()),
                "chatRoomId", "contextId", "recipientUserId", "recipientName", "title", "genre", "part");
    }

    @Test
    void documentsFanHomeOperationsInNotionOrderWithTypedResponses() throws Exception {
        JsonNode document = swaggerDocument();
        Map<String, List<String>> operations = new LinkedHashMap<>();
        document.path("paths").fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(entry -> {
            JsonNode operation = entry.getValue();
            if (!operation.at("/tags/0").asText().equals("팬모드_홈")) {
                return;
            }
            String method = entry.getKey();
            String location = method + " " + path.getKey();
            operations.computeIfAbsent(path.getKey(), key -> new ArrayList<>()).add(method);
            assertThat(operation.has("requestBody")).as(location).isFalse();
            assertThat(operation.at("/security/0").has("bearerAuth")).as(location).isTrue();
            assertThat(operation.at("/responses/401/$ref").asText()).as(location).endsWith("/AuthenticationRequired");
            JsonNode content = operation.path("responses").path(method.equals("post") ? "201" : "200")
                    .at("/content/application~1json");
            assertObjectSchema(document, content.path("schema"), location);
            assertObjectExamples(content);
            JsonNode result = resolve(document, content.path("schema")).at("/properties/result");
            assertThat(resolve(document, result).path("type").asText()).as(location)
                    .isEqualTo(path.getKey().endsWith("/members/profiles") ? "array" : "object");
        }));
        assertThat(operations.keySet()).containsExactly(
                "/home", "/performances/participation/pending", "/performances/{performanceId}/alarm",
                "/performances/{performanceId}/interest", "/performances/{performanceId}/participation/complete",
                "/performances/{performanceId}/participation", "/performances/{performanceId}/detail",
                "/posts/following", "/performances/upcoming", "/performances/recommend", "/performances/calendar",
                "/performances/by-date", "/bands/{bandId}/members/profiles");
        assertThat(operations.get("/performances/{performanceId}/alarm")).containsExactlyInAnyOrder("post", "delete");
        assertThat(operations.get("/performances/{performanceId}/interest")).containsExactlyInAnyOrder("post", "delete");
        JsonNode upcoming = document.path("paths").path("/performances/upcoming").at("/get/parameters");
        assertThat(upcoming).extracting(parameter -> parameter.path("name").asText())
                .containsExactlyInAnyOrder("sort", "page", "size");
        assertThat(upcoming).filteredOn(parameter -> parameter.path("name").asText().equals("sort"))
                .singleElement().satisfies(sort -> assertThat(sort.at("/schema/default").asText()).isEqualTo("IMMINENT"));
    }

    @Test
    void fanHomeExamplesUseActualPerformanceAndParticipationResponses() throws Exception {
        JsonNode paths = swaggerDocument().path("paths");
        Band band = createAcceptedBand();
        String owner = "Bearer " + jwtUtil.createAccessToken(new AuthMember(band.getOwner()));
        String fan = bearer(UserRole.USER);
        String bandShows = "/bands/{bandId}/performances";
        ObjectNode request = requestExample(paths, bandShows).deepCopy();
        request.put("performanceDate", LocalDate.now().plusDays(10).toString());
        JsonNode created = compareBandResponse(paths, bandShows, "post",
                mvc.perform(post(bandShows, band.getId()).header(AUTHORIZATION, owner).contentType(APPLICATION_JSON)
                                .content(request.toString())).andExpect(status().isCreated()), "performanceId", "performanceDate");
        long showId = created.path("performanceId").asLong();
        String alarm = "/performances/{performanceId}/alarm";
        compareBandResponse(paths, alarm, "post",
                mvc.perform(post(alarm, showId).header(AUTHORIZATION, fan)).andExpect(status().isCreated()),
                "performanceId");
        mvc.perform(post(alarm, showId).header(AUTHORIZATION, fan))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SHOW409_1"));
        mvc.perform(get("/performances/{performanceId}/detail", showId).header(AUTHORIZATION, fan))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("SHOW200_10"))
                .andExpect(jsonPath("$.result.genre").value(Genre.valueOf(request.path("genre").asText()).getName()))
                .andExpect(jsonPath("$.result.region").value(Region.valueOf(request.path("region").asText()).getName()))
                .andExpect(jsonPath("$.result.participationStatus").value("SCHEDULED"))
                .andExpect(jsonPath("$.result.isInterested").value(true));
        compareBandResponse(paths, alarm, "delete",
                mvc.perform(delete(alarm, showId).header(AUTHORIZATION, fan)).andExpect(status().isOk()), "performanceId");
        mvc.perform(get("/home").header(AUTHORIZATION, fan)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("HOME200_1"))
                .andExpect(jsonPath("$.result.performanceType").value("RECOMMENDED"))
                .andExpect(jsonPath("$.result.hasFollowingBands").value(false));
        mvc.perform(get("/performances/calendar").header(AUTHORIZATION, fan).param("year", "2026").param("month", "13"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_3"));
    }

    @Test
    void documentsExploreOperationsWithTypedJsonAndActualParameters() throws Exception {
        JsonNode document = swaggerDocument();
        JsonNode paths = document.path("paths");
        List<String> operations = new ArrayList<>();
        paths.fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(method -> {
            JsonNode operation = method.getValue();
            if (!operation.at("/tags/0").asText().equals("팬모드_탐색")) {
                return;
            }
            String location = method.getKey() + " " + path.getKey();
            operations.add(location);
            assertThat(operation.at("/security/0").has("bearerAuth")).as(location).isTrue();
            boolean hasBody = location.equals("post /posts/{postId}/comments")
                    || location.equals("patch /posts/{postId}/comments/{commentId}");
            assertThat(operation.has("requestBody")).as(location).isEqualTo(hasBody);
            if (hasBody) {
                JsonNode body = operation.at("/requestBody/content/application~1json");
                assertObjectSchema(document, body.path("schema"), location);
                assertObjectExamples(body);
                assertThat(mediaExample(body).path("content").asText()).isNotBlank();
            }
            JsonNode response = operation.path("responses")
                    .path(location.equals("post /bands/{bandId}/follow") ? "201" : "200")
                    .at("/content/application~1json");
            assertObjectSchema(document, response.path("schema"), location);
            assertObjectExamples(response);
            assertThat(resolve(document, response.path("schema")).path("properties").has("result")).as(location).isTrue();
        }));
        assertThat(operations).containsExactlyInAnyOrder(
                "get /bands/recommendations",
                "get /explore/search",
                "post /bands/{bandId}/follow",
                "delete /bands/{bandId}/follow",
                "get /explore/search/recent",
                "delete /explore/search/recent/{recentSearchId}",
                "delete /explore/search/recent",
                "get /bands/{bandId}/detail",
                "get /posts/{postId}/detail",
                "get /posts/{postId}/comments",
                "post /posts/{postId}/comments",
                "patch /posts/{postId}/comments/{commentId}",
                "delete /posts/{postId}/comments/{commentId}",
                "post /posts/{postId}/likes",
                "delete /posts/{postId}/likes");
        JsonNode search = paths.path("/explore/search").path("get");
        assertThat(search.path("parameters")).extracting(parameter -> parameter.path("name").asText())
                .containsExactlyInAnyOrder("keyword", "type", "sort", "genre", "region", "withDummy", "cursor", "size");
        assertThat(search.path("parameters")).filteredOn(parameter -> parameter.path("name").asText().equals("withDummy"))
                .singleElement().satisfies(parameter -> assertThat(parameter.at("/schema/default").asBoolean()).isTrue());
    }

    @Test
    void exploreCommentAndSearchExamplesMatchHttpResponses() throws Exception {
        JsonNode paths = swaggerDocument().path("paths");
        Band band = createAcceptedBand();
        String owner = "Bearer " + jwtUtil.createAccessToken(new AuthMember(band.getOwner()));
        String token = bearer(UserRole.USER);
        String bandPosts = "/bands/{bandId}/posts";
        JsonNode created = compareBandResponse(paths, bandPosts, "post",
                mvc.perform(post(bandPosts, band.getId()).header(AUTHORIZATION, owner).contentType(APPLICATION_JSON)
                                .content(requestExample(paths, bandPosts).toString()))
                        .andExpect(status().isOk()), "postId", "createdAt");
        long postId = created.path("postId").asLong();
        String comments = "/posts/{postId}/comments";
        JsonNode comment = compareBandResponse(paths, comments, "post",
                mvc.perform(post(comments, postId).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(requestExample(paths, comments).toString()))
                        .andExpect(status().isOk()), "commentId", "createdAt");
        long commentId = comment.path("commentId").asLong();
        String commentPath = comments + "/{commentId}";
        JsonNode edit = mediaExample(paths.path(commentPath).at("/patch/requestBody/content/application~1json"));
        compareBandResponse(paths, commentPath, "patch",
                mvc.perform(patch(commentPath, postId, commentId).header(AUTHORIZATION, token)
                                .contentType(APPLICATION_JSON).content(edit.toString()))
                        .andExpect(status().isOk()), "commentId", "createdAt");
        compareBandResponse(paths, commentPath, "delete",
                mvc.perform(delete(commentPath, postId, commentId).header(AUTHORIZATION, token))
                        .andExpect(status().isOk()), "commentId");

        mvc.perform(get("/explore/search").header(AUTHORIZATION, token).param("keyword", " "))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SEARCH400_1"));
        String recent = "/explore/search/recent";
        mvc.perform(get(recent).header(AUTHORIZATION, token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.recentSearches").isEmpty());
        compareResponse(paths, recent, "delete",
                mvc.perform(delete(recent).header(AUTHORIZATION, token)).andExpect(status().isOk()));
    }

    @Test
    void documentsFanLiveOperationsWithTypedJsonAndServerSentEvents() throws Exception {
        JsonNode document = swaggerDocument();
        JsonNode paths = document.path("paths");
        List<String> operations = new ArrayList<>();
        paths.fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(entry -> {
            JsonNode operation = entry.getValue();
            if (!operation.at("/tags/0").asText().equals("팬모드_라이브")) {
                return;
            }
            String location = entry.getKey() + " " + path.getKey();
            operations.add(location);
            assertThat(operation.at("/security/0").has("bearerAuth")).as(location).isTrue();
            if (path.getKey().endsWith("/viewers")) {
                JsonNode events = operation.at("/responses/200/content/text~1event-stream");
                assertThat(mediaExample(events).asText()).contains("event:viewerCount", "data:3", ":ping");
                assertThat(operation.at("/responses/200/content/application~1json").isMissingNode()).isTrue();
                return;
            }
            if (path.getKey().endsWith("/replay/playlist")) {
                JsonNode playlist = operation.at("/responses/200/content/application~1vnd.apple.mpegurl");
                assertThat(playlist.at("/schema/type").asText()).isEqualTo("string");
                assertThat(mediaExample(playlist).asText()).startsWith("#EXTM3U").contains("#EXT-X-ENDLIST");
                return;
            }
            boolean hasBody = path.getKey().endsWith("/reports") || path.getKey().equals("/users/blocks");
            assertThat(operation.has("requestBody")).as(location).isEqualTo(hasBody);
            if (hasBody) {
                JsonNode body = operation.at("/requestBody/content/application~1json");
                assertObjectSchema(document, body.path("schema"), location);
                assertObjectExamples(body);
            }
            JsonNode response = operation.path("responses").path(path.getKey().endsWith("/reports") ? "201" : "200")
                    .at("/content/application~1json");
            assertObjectSchema(document, response.path("schema"), location);
            assertObjectExamples(response);
        }));
        assertThat(operations).containsExactlyInAnyOrder(
                "get /lives/home",
                "post /lives/{liveId}",
                "get /lives/{liveId}/viewers",
                "post /lives/{liveId}/leave",
                "post /lives/{liveId}/reports",
                "post /users/blocks",
                "delete /users/blocks",
                "get /lives/live-now/following",
                "get /lives/live-now/all",
                "get /lives/scheduled",
                "post /lives/{liveId}/alarm",
                "get /lives/replays/following",
                "get /lives/replays/all",
                "get /lives/{liveId}/replay",
                "get /lives/{liveId}/replay/playlist");
        JsonNode home = paths.path("/lives/home").at("/get/responses/200/content/application~1json/schema");
        assertThat(resolve(document, home).at("/properties/result/oneOf").size()).isEqualTo(2);
        JsonNode replays = paths.path("/lives/replays/all").at("/get/responses/200/content/application~1json/schema");
        JsonNode replayPage = resolve(document, resolve(document, replays).at("/properties/result"));
        assertThat(replayPage.at("/properties/items/type").asText()).isEqualTo("array");
        assertThat(replayPage.at("/properties/pageInfo/$ref").asText()).endsWith("/PageInfo");
    }

    @Test
    void replayPlaylistReturnsManifestAndRejectsMissingReplay() throws Exception {
        Band band = createAcceptedBand();
        AudioStream stream = audioStreams.save(AudioStream.builder()
                .broadcasterId(band.getOwner().getId()).bandId(band.getId()).path("playlist-swagger-test")
                .title("다시보기").status(StreamStatus.CLOSED).build());
        streamReplays.save(StreamReplay.builder().audioStream(stream).s3Key("replays/test/segment.mp4")
                .durationSec(32).build());
        String path = "/lives/{liveId}/replay/playlist";
        String token = bearer(UserRole.USER);
        mvc.perform(get(path, stream.getId()).header(AUTHORIZATION, token).accept("application/vnd.apple.mpegurl"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/vnd.apple.mpegurl"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .startsWith("#EXTM3U").contains("#EXTINF:32.0,", "#EXT-X-ENDLIST"));
        mvc.perform(get(path, Long.MAX_VALUE).header(AUTHORIZATION, token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("LIVE404_3"));
        mvc.perform(get(path, stream.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void fanLiveAlarmReportAndBlockExamplesMatchActualHttpResponses() throws Exception {
        JsonNode paths = swaggerDocument().path("paths");
        Band band = createAcceptedBand();
        User target = band.getOwner();
        AudioStream stream = audioStreams.save(AudioStream.builder()
                .broadcasterId(target.getId()).bandId(band.getId()).path("fan-live-swagger-test")
                .title("신곡 데모 첫 공개!").status(StreamStatus.SCHEDULED)
                .scheduledAt(LocalDateTime.now().plusDays(1)).build());
        long liveId = stream.getId();
        String token = bearer(UserRole.USER);

        String reports = "/lives/{liveId}/reports";
        ObjectNode report = requestExample(paths, reports).deepCopy();
        report.put("targetUserId", target.getId());
        compareBandResponse(paths, reports, "post",
                mvc.perform(post(reports, liveId).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(report.toString())).andExpect(status().isCreated()), "targetUserId");

        String blocks = "/users/blocks";
        ObjectNode block = requestExample(paths, blocks).deepCopy();
        block.put("targetUserId", target.getId());
        block.put("liveId", liveId);
        compareResponse(paths, blocks, "post",
                mvc.perform(post(blocks).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(block.toString())).andExpect(status().isOk()));
        compareResponse(paths, blocks, "delete",
                mvc.perform(delete(blocks).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(block.toString())).andExpect(status().isOk()));

        String alarm = "/lives/{liveId}/alarm";
        compareResponse(paths, alarm, "post", mvc.perform(post(alarm, liveId).header(AUTHORIZATION, token))
                .andExpect(status().isOk()));
        mvc.perform(get("/lives/replays/following").header(AUTHORIZATION, token).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.items").isEmpty())
                .andExpect(jsonPath("$.result.pageInfo.hasNext").value(false));
        mvc.perform(get("/lives/{liveId}/viewers", liveId).header(AUTHORIZATION, token)
                        .accept(TEXT_EVENT_STREAM))
                .andExpect(status().isNotFound()).andExpect(header().string("X-Accel-Buffering", "no"))
                .andExpect(content().string(""));
    }

    @Test
    void documentsBandLiveRequestsAndSharedOperations() throws Exception {
        JsonNode document = swaggerDocument();
        JsonNode paths = document.path("paths");
        List<String> operations = new ArrayList<>();
        paths.fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(entry -> {
            JsonNode operation = entry.getValue();
            boolean bandLive = false;
            for (JsonNode tag : operation.path("tags")) {
                bandLive |= tag.asText().equals("밴드모드_라이브");
            }
            if (!bandLive) {
                return;
            }
            String location = entry.getKey() + " " + path.getKey();
            operations.add(location);
            assertThat(operation.at("/security/0").has("bearerAuth")).as(location).isTrue();
            boolean hasBody = List.of("post /lives", "post /lives/{liveId}/co-host/acceptance",
                    "patch /lives/{liveId}/reservation", "patch /lives/{liveId}/co-host-invitation").contains(location);
            assertThat(operation.has("requestBody")).as(location).isEqualTo(hasBody);
            if (hasBody) {
                JsonNode body = operation.at("/requestBody/content/application~1json");
                assertObjectSchema(document, body.path("schema"), location);
                assertObjectExamples(body);
            }
            if (location.endsWith("/viewers")) {
                assertThat(operation.at("/responses/200/content/text~1event-stream/examples/공동 송출 요청/value").asText())
                        .contains("coHostUpgradeRequested");
                return;
            }
            String status = switch (location) {
                case "post /lives", "post /lives/{liveId}/co-host" -> "201";
                case "post /lives/{liveId}/replay" -> "202";
                default -> "200";
            };
            JsonNode response = operation.path("responses").path(status).at("/content/application~1json");
            assertObjectSchema(document, response.path("schema"), location);
            assertObjectExamples(response);
        }));
        assertThat(operations).containsExactlyInAnyOrder(
                "get /lives/home",
                "post /lives",
                "post /lives/{liveId}",
                "post /lives/{liveId}/co-host",
                "post /lives/{liveId}/co-host/acceptance",
                "get /lives/{liveId}/viewers",
                "get /lives/{liveId}/members",
                "post /lives/{liveId}/close",
                "get /lives/{liveId}/summary",
                "post /lives/{liveId}/replay",
                "get /lives/{liveId}/reservation",
                "patch /lives/{liveId}/reservation",
                "delete /lives/{liveId}/reservation",
                "patch /lives/{liveId}/co-host-invitation");
        assertThat(paths.path("/lives/{liveId}").at("/post/responses/200/content/application~1json/examples/밴드 송출자/value")
                .at("/result/playback/protocol").asText()).isEqualTo("WHIP");
        assertThat(paths.path("/lives/{liveId}/replay").at("/get/tags/0").asText()).isEqualTo("팬모드_라이브");
    }

    @Test
    void bandLiveReservationExamplesMatchActualHttpResponses() throws Exception {
        JsonNode paths = swaggerDocument().path("paths");
        Band band = createAcceptedBand();
        User owner = band.getOwner();
        owner.changeMode(UserMode.BAND);
        BandMemberProfile active = bandMemberProfiles.save(BandMemberProfile.builder()
                .user(owner).nickname("밴드리더").part(Part.VOCAL).active(true).build());
        bandMembers.findByBand_IdAndUser_Id(band.getId(), owner.getId()).orElseThrow().acceptWithProfile(active);
        String token = "Bearer " + jwtUtil.createAccessToken(new AuthMember(owner));

        ObjectNode reservation = requestExample(paths, "/lives").deepCopy();
        reservation.put("scheduledAt", LocalDateTime.now().plusDays(2).withNano(0).toString());
        JsonNode created = compareBandResponse(paths, "/lives", "post",
                mvc.perform(post("/lives").header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(reservation.toString())).andExpect(status().isCreated()), "audioStreamId", "path");
        long liveId = created.path("audioStreamId").asLong();
        String path = "/lives/{liveId}/reservation";
        mvc.perform(get(path, liveId).header(AUTHORIZATION, token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LIVE200_11"))
                .andExpect(jsonPath("$.result.coHostCandidates[0].status").value("OWNER"))
                .andExpect(jsonPath("$.result.coHostCandidates[0].part").value("보컬"));
        ObjectNode edit = (ObjectNode) mediaExample(paths.path(path).at("/patch/requestBody/content/application~1json")).deepCopy();
        edit.put("scheduledAt", LocalDateTime.now().plusDays(3).withNano(0).toString());
        compareResponse(paths, path, "patch",
                mvc.perform(patch(path, liveId).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(edit.toString())).andExpect(status().isOk()));
        mvc.perform(get(path, liveId).header(AUTHORIZATION, token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.title").value(edit.path("title").asText()));
        mvc.perform(post("/lives/{liveId}/replay", liveId).header(AUTHORIZATION, token))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LIVE409_4"));
        compareResponse(paths, path, "delete",
                mvc.perform(delete(path, liveId).header(AUTHORIZATION, token)).andExpect(status().isOk()));
    }

    @Test
    void documentsMediaAndNotificationOperationsWithTypedJson() throws Exception {
        JsonNode document = swaggerDocument();
        JsonNode paths = document.path("paths");
        List<String> operations = new ArrayList<>();
        paths.fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(entry -> {
            JsonNode operation = entry.getValue();
            if (!List.of("미디어 업로드 (공통)", "알림").contains(operation.at("/tags/0").asText())) {
                return;
            }
            String location = entry.getKey() + " " + path.getKey();
            operations.add(location);
            assertThat(operation.at("/security/0").has("bearerAuth")).as(location).isTrue();
            boolean hasBody = List.of("post /media/presigned-url", "post /notifications/tokens",
                    "delete /notifications/tokens", "post /notifications/test",
                    "patch /users/me/notification-settings/{settingType}").contains(location);
            assertThat(operation.has("requestBody")).as(location).isEqualTo(hasBody);
            if (hasBody) {
                JsonNode request = operation.at("/requestBody/content/application~1json");
                assertObjectSchema(document, request.path("schema"), location);
                assertObjectExamples(request);
            }
            JsonNode response = operation.at("/responses/200/content/application~1json");
            assertObjectSchema(document, response.path("schema"), location);
            assertObjectExamples(response);
        }));
        assertThat(operations).containsExactlyInAnyOrder(
                "post /media/presigned-url", "post /notifications/tokens", "delete /notifications/tokens",
                "post /notifications/test", "patch /notifications/{notificationId}/read",
                "get /users/me/notification-settings", "patch /users/me/notification-settings/{settingType}",
                "get /notifications");
        JsonNode list = paths.path("/notifications").path("get");
        assertThat(list.path("parameters")).extracting(parameter -> parameter.path("name").asText())
                .containsExactlyInAnyOrder("cursor", "size");
        assertThat(list.path("parameters")).filteredOn(parameter -> parameter.path("name").asText().equals("size"))
                .singleElement().satisfies(size -> assertThat(size.at("/schema/default").asInt()).isEqualTo(20));
        JsonNode response = list.at("/responses/200/content/application~1json/schema");
        JsonNode page = resolve(document, resolve(document, response).at("/properties/result"));
        assertThat(page.at("/properties/items/type").asText()).isEqualTo("array");
        assertThat(page.at("/properties/pageInfo/$ref").asText()).endsWith("/PageInfo");
    }

    @Test
    void mediaUploadExamplesMatchActualPresignedUrlResponse() throws Exception {
        JsonNode paths = swaggerDocument().path("paths");
        String token = bearer(UserRole.USER);
        String path = "/media/presigned-url";
        JsonNode result = compareBandResponse(paths, path, "post",
                mvc.perform(post(path).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(requestExample(paths, path).toString())).andExpect(status().isOk()),
                "presignedUrl", "fileUrl");
        URI signed = URI.create(result.path("presignedUrl").asText());
        URI file = URI.create(result.path("fileUrl").asText());
        assertThat(signed.getPath()).isEqualTo(file.getPath()).startsWith("/post/").endsWith(".mp4");
        assertThat(signed.getQuery()).contains("X-Amz-Expires=300", "X-Amz-Signature=");
        assertThat(file.getQuery()).isNull();
        mvc.perform(post(path).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                        .content("""
                                {"category":"SESSION_PROFILE","fileName":"profile.gif","contentType":"image/gif"}
                                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_3"));
    }

    @Test
    void notificationExamplesMatchActualTokenSettingsAndReadResponses() throws Exception {
        JsonNode paths = swaggerDocument().path("paths");
        User user = users.save(User.builder().name("알림테스트").role(UserRole.USER)
                .birthDate(LocalDate.of(2000, 1, 1)).gender(Gender.FEMALE).build());
        String token = "Bearer " + jwtUtil.createAccessToken(new AuthMember(user));
        String tokens = "/notifications/tokens";
        compareResponse(paths, tokens, "post",
                mvc.perform(post(tokens).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(requestExample(paths, tokens).toString())).andExpect(status().isOk()));
        compareResponse(paths, tokens, "delete",
                mvc.perform(delete(tokens).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(mediaExample(paths.path(tokens).at("/delete/requestBody/content/application~1json")).toString()))
                        .andExpect(status().isOk()));
        String testPush = "/notifications/test";
        compareResponse(paths, testPush, "post",
                mvc.perform(post(testPush).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(requestExample(paths, testPush).toString())).andExpect(status().isOk()));

        String settings = "/users/me/notification-settings";
        compareResponse(paths, settings, "get",
                mvc.perform(get(settings).header(AUTHORIZATION, token).param("mode", "FAN"))
                        .andExpect(status().isOk()));
        String setting = settings + "/{settingType}";
        compareResponse(paths, setting, "patch",
                mvc.perform(patch(setting, "FAN_LIVE_REPLAY_READY").header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                                .content(mediaExample(paths.path(setting).at("/patch/requestBody/content/application~1json")).toString()))
                        .andExpect(status().isOk()));

        Notification unread = notifications.save(Notification.builder().user(user).type(NotificationType.SESSION)
                .title("새 세션 지원서").body("새로운 지원서가 도착했어요.").build());
        Notification newerRead = notifications.save(Notification.builder().user(user).type(NotificationType.LIVE)
                .title("라이브 시작").body("라이브가 시작됐어요.").build());
        newerRead.markAsRead();
        mvc.perform(get("/notifications").header(AUTHORIZATION, token).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.items[0].notificationId").value(unread.getId()))
                .andExpect(jsonPath("$.result.pageInfo.nextCursor").value(unread.getId()));
        mvc.perform(get("/notifications").header(AUTHORIZATION, token).param("size", "1")
                        .param("cursor", unread.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.items[0].notificationId").value(newerRead.getId()))
                .andExpect(jsonPath("$.result.pageInfo.hasNext").value(false));
        String read = "/notifications/{notificationId}/read";
        compareResponse(paths, read, "patch",
                mvc.perform(patch(read, unread.getId()).header(AUTHORIZATION, token)).andExpect(status().isOk()));
        assertThat(unread.getIsRead()).isTrue();
    }

    @Test
    void documentsMobileSdkSeparatelyWithTypedLoginBodies() throws Exception {
        JsonNode document = swaggerDocument();
        JsonNode paths = document.path("paths");
        Map<String, JsonNode> mobile = new LinkedHashMap<>();
        paths.fields().forEachRemaining(path -> {
            JsonNode operation = path.getValue().path("post");
            if (operation.at("/tags/0").asText().equals("모바일 SDK")) {
                mobile.put(path.getKey(), operation);
            }
        });
        assertThat(mobile.keySet()).containsExactly(
                "/auth/oauth/mobile/kakao", "/auth/oauth/mobile/apple", "/auth/oauth/mobile/apple/challenge");
        mobile.forEach((path, operation) -> {
            assertThat(operation.has("security")).as(path).isFalse();
            JsonNode request = operation.at("/requestBody/content/application~1json");
            assertObjectSchema(document, request.path("schema"), path + " request");
            assertObjectExamples(request);
            JsonNode response = operation.at("/responses/200/content/application~1json");
            assertObjectSchema(document, response.path("schema"), path + " response");
            assertObjectExamples(response);
            assertThat(operation.at("/responses/429/headers/Retry-After/schema/type").asText()).isEqualTo("integer");
        });
        assertThat(requestExample(paths, "/auth/oauth/mobile/kakao").path("accessToken").isTextual()).isTrue();
        assertThat(requestExample(paths, "/auth/oauth/mobile/apple").fieldNames()).toIterable()
                .containsExactlyInAnyOrder("identityToken", "authorizationCode", "challengeId");
        assertThat(requestExample(paths, "/auth/oauth/mobile/apple/challenge").isEmpty()).isTrue();
    }

    @Test
    void documentsWebSocketTicketsAndHandshakesWithSeparateContracts() throws Exception {
        JsonNode document = swaggerDocument();
        JsonNode paths = document.path("paths");
        List<String> socketPaths = new ArrayList<>();
        paths.fields().forEachRemaining(path -> {
            if (path.getValue().elements().next().at("/tags/0").asText().equals("웹소켓")) {
                socketPaths.add(path.getKey());
            }
        });
        assertThat(socketPaths).containsExactly(
                "/lives/{liveId}/chat/ws-ticket", "/ws/lives/{liveId}/chat", "/chat/ws-ticket", "/ws/chat");
        Map<String, String> ticketFields = Map.of(
                "/lives/{liveId}/chat/ws-ticket", "expiresInSeconds", "/chat/ws-ticket", "expiresIn");
        for (var entry : ticketFields.entrySet()) {
            JsonNode operation = paths.path(entry.getKey()).path("post");
            assertThat(operation.at("/security/0/bearerAuth").isArray()).isTrue();
            assertThat(operation.has("requestBody")).isFalse();
            JsonNode media = operation.at("/responses/200/content/application~1json");
            assertObjectSchema(document, media.path("schema"), entry.getKey());
            assertThat(mediaExample(media).at("/result/" + entry.getValue()).asInt()).isEqualTo(30);
        }
        for (String path : List.of("/ws/lives/{liveId}/chat", "/ws/chat")) {
            JsonNode handshake = paths.path(path).path("get");
            String protocol = path.equals("/ws/chat") ? "dm.v1" : "live-chat.v1";
            assertThat(handshake.has("security")).isFalse();
            assertThat(handshake.at("/responses/101/headers/Sec-WebSocket-Protocol/schema/example").asText())
                    .isEqualTo(protocol);
            assertThat(handshake.at("/responses/101/content").isMissingNode()).isTrue();
            String description = handshake.path("description").asText();
            assertThat(description).contains("?ticket={ticket}", "\"type\":\"system.event\"", "\"type\":\"pong\"");
            assertThat(description).contains(path.equals("/ws/chat") ? "\"type\":\"dm.read\"" : "\"type\":\"live-chat.send\"");
        }
        JsonNode documentedError = document.at("/components/examples/LIVE_CHAT_NOT_OPEN/value");
        mvc.perform(post("/chat/ws-ticket")).andExpect(status().isUnauthorized());
        mvc.perform(post("/lives/{liveId}/chat/ws-ticket", Long.MAX_VALUE)
                        .header(AUTHORIZATION, bearer(UserRole.USER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(documentedError.path("code").asText()))
                .andExpect(jsonPath("$.message").value(documentedError.path("message").asText()));
    }

    private Band createAcceptedBand() {
        User owner = users.save(User.builder().name("테스트")
                .birthDate(LocalDate.of(2000, 1, 1)).gender(Gender.FEMALE).build());
        Band band = bands.save(Band.builder().owner(owner).name("APPLE").genre(Genre.INDIE)
                .region(Region.SEOUL).status(BandStatus.ACCEPTED).build());
        bandMembers.save(BandMember.builder().band(band).user(owner).status(BandMemberStatus.ACCEPTED).build());
        return band;
    }

    @Test
    void swaggerExamplesMatchActualOnboardingAndMyResponses() throws Exception {
        JsonNode paths = swaggerDocument().path("paths");
        compareResponse(paths, "/genres", "get", mvc.perform(get("/genres")).andExpect(status().isOk()));
        compareResponse(paths, "/regions", "get", mvc.perform(get("/regions")).andExpect(status().isOk()));

        String token = bearer(UserRole.USER);
        JsonNode onboarding = paths.path("/onboarding").at("/patch/requestBody/content/application~1json");
        mvc.perform(patch("/onboarding").header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                        .content(onboarding.at("/examples/팬 정보 포함/value").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ONBOARDING200_3"))
                .andExpect(jsonPath("$.result.completed").value(true))
                .andExpect(jsonPath("$.result.selectedGenres[0].code").value("METAL"));

        JsonNode myInfo = paths.path("/users/me/information").at("/patch/requestBody/content/application~1json");
        String myInfoBody = mediaExample(myInfo).toString();
        compareResponse(paths, "/users/me/information", "patch",
                mvc.perform(patch("/users/me/information").header(AUTHORIZATION, token)
                                .contentType(APPLICATION_JSON).content(myInfoBody))
                        .andExpect(status().isOk()));

        compareResponse(paths, "/users/me/mode", "put",
                mvc.perform(put("/users/me/mode").header(AUTHORIZATION, token))
                        .andExpect(status().isOk()));
    }

    private JsonNode swaggerDocument() throws Exception {
        String body = mvc.perform(get(DOCS).with(httpBasic("bscene-admin", "test-swagger-credential")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(body);
    }

    private void compareResponse(JsonNode paths, String path, String method, ResultActions actions) throws Exception {
        var response = actions.andReturn().getResponse();
        JsonNode actual = new ObjectMapper().readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        JsonNode content = paths.path(path).path(method).path("responses")
                .path(Integer.toString(response.getStatus())).at("/content/application~1json");
        JsonNode expected = mediaExample(content);
        for (String field : List.of("isSuccess", "status", "code", "message", "result")) {
            assertThat(actual.path(field)).as("%s %s %s", method, path, field).isEqualTo(expected.path(field));
        }
    }

    private JsonNode compareBandResponse(JsonNode paths, String path, String method, ResultActions actions,
                                         String... generatedFields) throws Exception {
        var response = actions.andReturn().getResponse();
        JsonNode actual = new ObjectMapper().readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        JsonNode expected = mediaExample(paths.path(path).path(method).path("responses")
                .path(Integer.toString(response.getStatus())).at("/content/application~1json"));
        for (String field : List.of("isSuccess", "status", "code", "message")) {
            assertThat(actual.path(field)).as("%s %s", path, field).isEqualTo(expected.path(field));
        }
        ObjectNode actualResult = (ObjectNode) actual.path("result");
        ObjectNode documentedResult = (ObjectNode) expected.path("result");
        assertThat(actualResult.fieldNames()).toIterable()
                .containsExactlyInAnyOrderElementsOf(() -> documentedResult.fieldNames());
        for (String field : generatedFields) {
            assertThat(actualResult.path(field).getNodeType()).as("%s %s", path, field)
                    .isEqualTo(documentedResult.path(field).getNodeType());
        }
        ObjectNode actualStable = actualResult.deepCopy();
        ObjectNode documentedStable = documentedResult.deepCopy();
        for (String field : generatedFields) {
            actualStable.remove(field);
            documentedStable.remove(field);
        }
        assertThat(actualStable).as("%s result", path).isEqualTo(documentedStable);
        return actualResult;
    }

    @Test
    void servesSwaggerUiAndConfigUnderTheApiContextPath() throws Exception {
        var credentials = httpBasic("bscene-admin", "test-swagger-credential");
        mvc.perform(get("/api/swagger-ui.html").contextPath("/api").with(credentials))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/api/swagger-ui/index.html"));
        mvc.perform(get("/api/swagger-ui/index.html").contextPath("/api").with(credentials))
                .andExpect(status().isOk());
        mvc.perform(get("/api/swagger-ui/swagger-ui-bundle.js").contextPath("/api").with(credentials))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v3/api-docs/swagger-config").contextPath("/api").with(credentials))
                .andExpect(status().isOk());
    }

    private static void assertObjectSchema(JsonNode document, JsonNode schema, String location) {
        assertThat(resolve(document, schema).path("type").asText())
                .as("%s schema %s", location, schema).isEqualTo("object");
    }

    private static void assertObjectExamples(JsonNode mediaType) {
        if (mediaType.has("example")) {
            assertThat(mediaType.path("example").isObject()).isTrue();
        } else {
            assertThat(mediaType.path("examples")).isNotEmpty();
            mediaType.path("examples").elements().forEachRemaining(example ->
                    assertThat(example.path("value").isObject()).isTrue());
        }
    }

    private static JsonNode resolve(JsonNode document, JsonNode schema) {
        return schema.has("$ref") ? document.at(schema.path("$ref").asText().substring(1)) : schema;
    }

    private static JsonNode requestExample(JsonNode paths, String path) {
        return mediaExample(paths.path(path).at("/post/requestBody/content/application~1json"));
    }

    private static JsonNode mediaExample(JsonNode media) {
        return media.has("example") ? media.path("example") : media.path("examples").elements().next().path("value");
    }

    private String bearer(UserRole role) {
        User user = users.save(User.builder().name("테스트")
                .birthDate(LocalDate.of(2000, 1, 1)).gender(Gender.FEMALE).role(role).build());
        return "Bearer " + jwtUtil.createAccessToken(new AuthMember(user));
    }
}

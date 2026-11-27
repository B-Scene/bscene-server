package com.umc.bscene.domain.session.controller;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.util.JwtUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class SessionProfileSearchApiTest {

    private static final String PROFILE = "/sessions/profile";
    private static final String SEARCH = "/sessions/applications/search";

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository users;
    @Autowired private SessionApplicationRepository applications;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private EntityManager entityManager;

    private MockMvc mvc;
    private User viewer;
    private String authorization;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        viewer = saveUser("세션 사용자");
        authorization = "Bearer " + jwtUtil.createAccessToken(new AuthMember(viewer));
    }

    @Test
    void updatesOwnNameAndRejectsInvalidInput() throws Exception {
        User other = saveUser("다른 사용자");
        mvc.perform(patch(PROFILE + "/name").header(AUTHORIZATION, authorization)
                        .contentType(APPLICATION_JSON).content("""
                        {"name":"  세진  ","userId":%d}
                        """.formatted(other.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SESSION200_25"))
                .andExpect(jsonPath("$.result.name").value("세진"));
        flushAndClear();
        mvc.perform(get(PROFILE).header(AUTHORIZATION, authorization))
                .andExpect(jsonPath("$.result.name").value("세진"));
        mvc.perform(get(PROFILE).header(AUTHORIZATION,
                        "Bearer " + jwtUtil.createAccessToken(new AuthMember(other))))
                .andExpect(jsonPath("$.result.name").value("다른 사용자"));

        for (String body : new String[]{"{}", "{\"name\":\"   \"}", "{\"name\":\"12345678901\"}"}) {
            mvc.perform(patch(PROFILE + "/name").header(AUTHORIZATION, authorization)
                            .contentType(APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SESSION400_10"));
        }
        mvc.perform(patch(PROFILE + "/name").header(AUTHORIZATION, authorization)
                        .contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_2"));
    }

    @Test
    void createsAndReplacesPhotoRejectsGifAndClearsPhoto() throws Exception {
        String first = "https://cdn.example.com/first.png";
        String replacement = "https://cdn.example.com/replacement.jpg";
        for (String url : new String[]{first, replacement}) {
            mvc.perform(patch(PROFILE + "/image").header(AUTHORIZATION, authorization)
                            .contentType(APPLICATION_JSON).content("""
                            {"profileImageUrl":"%s"}
                            """.formatted(url)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SESSION200_26"))
                    .andExpect(jsonPath("$.result.profileImageUrl").value(url));
            flushAndClear();
        }
        for (String body : new String[]{"{}", "{\"profileImageUrl\":\"not-a-url\"}",
                "{\"profileImageUrl\":\"https://cdn.example.com/photo.GIF?v=1\"}"}) {
            mvc.perform(patch(PROFILE + "/image").header(AUTHORIZATION, authorization)
                            .contentType(APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SESSION400_11"));
        }
        mvc.perform(get(PROFILE).header(AUTHORIZATION, authorization))
                .andExpect(jsonPath("$.result.profileImageUrl").value(replacement));
        mvc.perform(patch(PROFILE + "/image").header(AUTHORIZATION, authorization)
                        .contentType(APPLICATION_JSON).content("{\"profileImageUrl\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.profileImageUrl").isEmpty());
        flushAndClear();
        mvc.perform(get(PROFILE).header(AUTHORIZATION, authorization))
                .andExpect(jsonPath("$.result.profileImageUrl").isEmpty());
    }

    @Test
    void requiresAuthenticationAndOldPatchIsRemoved() throws Exception {
        mvc.perform(patch(PROFILE + "/name").contentType(APPLICATION_JSON).content("{\"name\":\"세진\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401_1"));
        mvc.perform(patch(PROFILE + "/image").header(AUTHORIZATION, "Bearer invalid")
                        .contentType(APPLICATION_JSON).content("{\"profileImageUrl\":\"\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(SEARCH)).andExpect(status().isUnauthorized());
        mvc.perform(patch(PROFILE).header(AUTHORIZATION, authorization)
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void blocksGifPresigningForSessionMedia() throws Exception {
        for (String category : new String[]{"SESSION_PROFILE", "SESSION_PORTFOLIO"}) {
            for (String fields : new String[]{
                    "\"fileName\":\"clip.GIF\",\"contentType\":\"application/octet-stream\"",
                    "\"fileName\":\"clip.png\",\"contentType\":\"image/gif\""
            }) {
                mvc.perform(post("/media/presigned-url").header(AUTHORIZATION, authorization)
                                .contentType(APPLICATION_JSON)
                                .content("{\"category\":\"" + category + "\"," + fields + "}"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("COMMON_400_3"));
            }
        }
    }

    @Test
    void sortsAndPagesSearchByDefaultApplicationButHonorsExplicitFilters() throws Exception {
        long both = saveApplication(saveUser("둘 다 일치"), Region.SEOUL, Genre.JAZZ);
        long one = saveApplication(saveUser("하나 일치"), Region.GYEONGGI, Genre.JAZZ);
        long none = saveApplication(saveUser("불일치"), Region.GYEONGGI, Genre.HARD_ROCK);
        mvc.perform(get(SEARCH).header(AUTHORIZATION, authorization).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].sessionApplicationId").value(none));

        saveApplication(viewer, Region.SEOUL, Genre.JAZZ);
        mvc.perform(get(SEARCH).header(AUTHORIZATION, authorization).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].sessionApplicationId").value(both))
                .andExpect(jsonPath("$.result.nextCursor").value(both));
        mvc.perform(get(SEARCH).header(AUTHORIZATION, authorization)
                        .param("cursorId", Long.toString(both)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(2))
                .andExpect(jsonPath("$.result.content[0].sessionApplicationId").value(one))
                .andExpect(jsonPath("$.result.content[1].sessionApplicationId").value(none))
                .andExpect(jsonPath("$.result.hasNext").value(false));
        mvc.perform(get(SEARCH).header(AUTHORIZATION, authorization).param("region", "GYEONGGI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].sessionApplicationId").value(none))
                .andExpect(jsonPath("$.result.content[1].sessionApplicationId").value(one));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private User saveUser(String name) {
        return users.save(User.builder().name(name).gender(Gender.FEMALE)
                .birthDate(LocalDate.of(2000, 1, 1)).build());
    }

    private long saveApplication(User user, Region region, Genre genre) {
        return applications.save(SessionApplication.builder()
                        .userId(user.getId()).nickname(user.getName()).title("기본 지원서")
                        .purpose("기본").part(Part.GUITAR).skillLevel(SkillLevel.INTERMEDIATE)
                        .region(region).genre(genre).build())
                .getSessionApplicationId();
    }
}

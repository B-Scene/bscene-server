package com.umc.bscene.domain.auth.controller;

import com.umc.bscene.domain.auth.dto.auth.request.LoginRequest;
import com.umc.bscene.domain.auth.dto.auth.request.LogoutRequest;
import com.umc.bscene.domain.auth.dto.auth.request.ReissueRequest;
import com.umc.bscene.domain.auth.dto.auth.response.ReissueResponse;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.auth.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthRouteProfileTest {

    private static final String LOGIN_BODY = """
            {"loginId":"qa@example.com","password":"qa-password"}
            """;
    private static final String REFRESH_BODY = """
            {"refreshToken":"service-refresh"}
            """;

    @ParameterizedTest
    @ValueSource(strings = {"dev", "qa"})
    void allowExistingAccountLoginInDevelopmentAndQa(String profile) {
        contextFor(profile).run(context -> {
            AuthService authService = context.getBean(AuthService.class);
            LoginRequest request = new LoginRequest("qa@example.com", "qa-password");
            when(authService.login(request)).thenReturn(
                    new TokenResponse("Bearer", "service-access", "service-refresh", 3600000L, null));

            mvc(context).perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.accessToken").value("service-access"));

            verify(authService).login(request);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "prod", "prod,dev", "prod,qa"})
    void hideLocalLoginWithoutQaProfileOrWhenProductionIsActive(String profiles) {
        contextFor(profiles).run(context -> {
            MockMvc mvc = mvc(context);

            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                    .andExpect(status().isNotFound());
            mvc.perform(post("/auth/login").principal(() -> "authenticated-user")
                            .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                    .andExpect(status().isNotFound());

            verifyNoInteractions(context.getBean(AuthService.class));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "qa", "prod"})
    void removeSignupRecoveryAndSmsRoutesForEveryUser(String profile) {
        contextFor(profile).run(context -> {
            MockMvc mvc = mvc(context);
            var requests = List.of(post("/auth/signup"), get("/auth/login-id/check"),
                    get("/auth/login-id/find"), patch("/auth/password-reset"),
                    post("/phone-verifications/send"), post("/phone-verifications/verify"));

            for (var request : requests) {
                mvc.perform(request).andExpect(status().isNotFound());
                mvc.perform(request.principal(() -> "authenticated-user")).andExpect(status().isNotFound());
            }

            verifyNoInteractions(context.getBean(AuthService.class));
        });
    }

    @Test
    void exposeSharedReissueAndLogoutInProduction() {
        contextFor("prod").run(context -> {
            AuthService authService = context.getBean(AuthService.class);
            when(authService.reissue(new ReissueRequest("service-refresh"))).thenReturn(
                    new ReissueResponse("Bearer", "new-access", "new-refresh", 3600000L));
            MockMvc mvc = mvc(context);

            mvc.perform(post("/auth/reissue").contentType(MediaType.APPLICATION_JSON).content(REFRESH_BODY))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result.accessToken").value("new-access"));
            mvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON).content(REFRESH_BODY))
                    .andExpect(status().isOk());

            verify(authService).reissue(new ReissueRequest("service-refresh"));
            verify(authService).logout(new LogoutRequest("service-refresh"));
        });
    }

    private WebApplicationContextRunner contextFor(String profiles) {
        return new WebApplicationContextRunner()
                .withUserConfiguration(MvcConfiguration.class)
                .withBean(AuthService.class, () -> mock(AuthService.class))
                .withPropertyValues("spring.profiles.active=" + profiles);
    }

    private MockMvc mvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({AuthController.class, LocalAuthController.class})
    static class MvcConfiguration {
    }
}

package com.umc.bscene.domain.oauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.auth.dto.auth.request.TermAgreementRequest;
import com.umc.bscene.domain.auth.dto.auth.response.LoginUserResponse;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.auth.entity.term.UserTerms;
import com.umc.bscene.domain.auth.enums.verification.PhoneVerificationPurpose;
import com.umc.bscene.domain.auth.repository.term.TermsRepository;
import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
import com.umc.bscene.domain.auth.service.verification.PhoneVerificationService;
import com.umc.bscene.domain.oauth.dto.request.OauthSignupRequest;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.entity.OauthAccount;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.dto.response.OAuthResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.entity.OAuthMember;
import com.umc.bscene.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OauthService {

    // 일회성 교환 코드 Redis 키 프리픽스 / TTL (짧게 — 리다이렉트 직후 바로 교환)
    private static final String EXCHANGE_KEY_PREFIX = "oauth:exchange:";
    private static final long EXCHANGE_CODE_TTL_SECONDS = 60L;

    private final UserRepository userRepository;
    private final OauthAccountRepository oauthAccountRepository;
    private final UserTermsRepository userTermsRepository;
    private final TermsRepository termsRepository;
    private final PhoneVerificationService phoneVerificationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 소셜 로그인 성공 처리: 기존 유저는 토큰 발급, 신규 유저는 임시 signup 토큰 발급
    @Transactional
    public OauthLoginResponse handleLoginSuccess(OAuthMember member) {
        if (member.isNewUser()) {
            OAuthResponse info = member.getOauthInfo();
            String signupToken = jwtUtil.createSignupToken(
                    info.getProvider().name(),
                    info.getProviderUid(),
                    info.getName(),
                    info.getEmail()
            );
            // email은 프론트가 온보딩 화면 아이디 칸을 채우는 용도로 함께 반환
            return OauthLoginResponse.ofNew(signupToken, info.getEmail());
        }

        return OauthLoginResponse.ofExisting(login(member.getUser()));
    }

    // 로그인 결과를 일회성 코드에 담아 Redis에 저장(짧은 TTL). 성공 핸들러가 프론트로 code만 리다이렉트
    public String issueExchangeCode(OauthLoginResponse result) {
        String code = UUID.randomUUID().toString();
        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(
                    EXCHANGE_KEY_PREFIX + code,
                    json,
                    Duration.ofSeconds(EXCHANGE_CODE_TTL_SECONDS)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("소셜 로그인 결과 직렬화에 실패했습니다.", e);
        }
        return code;
    }

    // 프론트가 code로 실제 로그인 결과를 교환(1회용: 조회와 동시에 삭제)
    public OauthLoginResponse exchangeCode(String code) {
        String json = stringRedisTemplate.opsForValue().getAndDelete(EXCHANGE_KEY_PREFIX + code);
        if (json == null) {
            throw new OauthException(OauthErrorCode.INVALID_EXCHANGE_CODE);
        }
        try {
            return objectMapper.readValue(json, OauthLoginResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("소셜 로그인 결과 역직렬화에 실패했습니다.", e);
        }
    }

    // 소셜 회원가입: 임시 토큰 + 온보딩 정보로 User + OauthAccount 생성 후 토큰 발급
    @Transactional
    public TokenResponse signup(OauthSignupRequest request) {
        String signupToken = request.signupToken();
        if (!jwtUtil.isValid(signupToken) || !"signup".equals(jwtUtil.getType(signupToken))) {
            throw new OauthException(OauthErrorCode.INVALID_SIGNUP_TOKEN);
        }

        SocialProvider provider;
        try {
            provider = SocialProvider.valueOf(jwtUtil.getProvider(signupToken));
        } catch (IllegalArgumentException e) {
            throw new OauthException(OauthErrorCode.NOT_SUPPORT_PROVIDER);
        }
        String providerUid = jwtUtil.getProviderUid(signupToken);
        // 이메일(아이디)은 사용자가 수정할 수 없으므로 요청 본문이 아닌 서명된 토큰에서 읽음
        String email = jwtUtil.getEmail(signupToken);

        if (oauthAccountRepository.findByProviderAndProviderUid(provider, providerUid).isPresent()) {
            throw new OauthException(OauthErrorCode.ALREADY_REGISTERED);
        }

        // 휴대폰 인증 완료 여부 검증 (로컬 회원가입과 동일)
        phoneVerificationService.validateVerified(PhoneVerificationPurpose.SIGNUP, request.phone());

        // phone은 dev 병합으로 unique 제약이 걸려 있어, 저장 전에 중복을 걸러 명확한 409 반환
        if (userRepository.existsByPhone(request.phone())) {
            throw new OauthException(OauthErrorCode.DUPLICATE_PHONE);
        }

        Gender gender = convertGender(request.genderCode());
        LocalDate birthDate = convertBirthDate(request.birthDatePrefix(), request.genderCode());

        User user = User.builder()
                .name(request.name())
                .gender(gender)
                .birthDate(birthDate)
                .phone(request.phone())
                .build();
        User savedUser = userRepository.save(user);

        OauthAccount oauthAccount = OauthAccount.builder()
                .user(savedUser)
                .provider(provider)
                .providerUid(providerUid)
                .email(email)
                .build();
        oauthAccountRepository.save(oauthAccount);

        saveTerms(savedUser, request.terms());

        // 인증 완료 상태 정리 (재사용 방지)
        phoneVerificationService.removeVerified(PhoneVerificationPurpose.SIGNUP, request.phone());

        return login(savedUser);
    }

    // 약관 동의 저장 (로컬 회원가입과 동일하게 termId 기반)
    private void saveTerms(User user, List<TermAgreementRequest> terms) {
        List<UserTerms> entities = terms.stream()
                .map(term -> UserTerms.builder()
                        .user(user)
                        .terms(termsRepository.getReferenceById(term.termId()))
                        .isAgreed(term.agreed())
                        .agreedAt(LocalDateTime.now())
                        .build())
                .toList();
        userTermsRepository.saveAll(entities);
    }

    // access/refresh 토큰 발급 + RefreshToken 저장
    private TokenResponse login(User user) {
        AuthMember authMember = new AuthMember(user);
        String accessToken = jwtUtil.createAccessToken(authMember);
        String refreshToken = jwtUtil.createRefreshToken(authMember);

        saveRefreshToken(user, refreshToken);

        LoginUserResponse loginUserResponse = new LoginUserResponse(
                user.getId(),
                user.getName(),
                user.getCurrentMode(),
                user.getOnboardingCompleted()
        );

        return new TokenResponse(
                "Bearer",
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                loginUserResponse
        );
    }

    private void saveRefreshToken(User user, String refreshToken) {
        String refreshTokenHash = hashToken(refreshToken);
        stringRedisTemplate.opsForValue().set(
                "refreshToken:" + refreshTokenHash,
                String.valueOf(user.getId()),
                Duration.ofMillis(jwtUtil.getRefreshTokenExpiration())
        );
    }

    // TODO: AuthService의 동일 로직과 중복 → 추후 공용 유틸로 추출 제안
    private Gender convertGender(String genderCode) {
        return switch (genderCode) {
            case "1", "3" -> Gender.MALE;
            case "2", "4" -> Gender.FEMALE;
            default -> throw new OauthException(OauthErrorCode.INVALID_SIGNUP_INFO);
        };
    }

    private LocalDate convertBirthDate(String birthDatePrefix, String genderCode) {
        int yearPrefix = switch (genderCode) {
            case "1", "2" -> 1900;
            case "3", "4" -> 2000;
            default -> throw new OauthException(OauthErrorCode.INVALID_SIGNUP_INFO);
        };

        int year = yearPrefix + Integer.parseInt(birthDatePrefix.substring(0, 2));
        int month = Integer.parseInt(birthDatePrefix.substring(2, 4));
        int day = Integer.parseInt(birthDatePrefix.substring(4, 6));

        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            throw new OauthException(OauthErrorCode.INVALID_SIGNUP_INFO);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashedToken = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedToken);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("토큰 해시에 실패했습니다.", e);
        }
    }
}

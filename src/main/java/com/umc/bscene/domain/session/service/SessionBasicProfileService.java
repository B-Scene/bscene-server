package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.auth.entity.credential.LocalCredential;
import com.umc.bscene.domain.auth.repository.credential.LocalCredentialRepository;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.session.dto.profile.request.SessionBasicProfileUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.response.SessionBasicProfileResponse;
import com.umc.bscene.domain.session.entity.SessionBasicProfile;
import com.umc.bscene.domain.session.repository.SessionBasicProfileRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionBasicProfileService {

    private final UserRepository userRepository;
    private final SessionBasicProfileRepository sessionBasicProfileRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final OauthAccountRepository oauthAccountRepository;

    public SessionBasicProfileResponse getProfile(Long userId) {
        User user = getUser(userId);
        SessionBasicProfile profile = sessionBasicProfileRepository
                .findByUser_Id(userId)
                .orElse(null);
        return SessionBasicProfileResponse.of(user, profile, findAccountEmail(userId));
    }

    @Transactional
    public SessionBasicProfileResponse updateProfile(
            Long userId,
            SessionBasicProfileUpdateRequest request
    ) {
        User user = getUser(userId);
        SessionBasicProfile profile = sessionBasicProfileRepository
                .findByUser_Id(userId)
                .orElseGet(() -> SessionBasicProfile.builder()
                        .user(user)
                        .build());

        profile.update(
                request.getEmail(),
                request.getGender(),
                request.getProfileImageUrl()
        );

        SessionBasicProfile savedProfile = sessionBasicProfileRepository.save(profile);
        return SessionBasicProfileResponse.of(
                user,
                savedProfile,
                findAccountEmail(userId)
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.UNAUTHORIZED_ERROR));
    }

    private String findAccountEmail(Long userId) {
        String localEmail = localCredentialRepository.findByUser_Id(userId)
                .map(LocalCredential::getLoginId)
                .filter(loginId -> loginId.contains("@"))
                .orElse(null);
        if (localEmail != null) {
            return localEmail;
        }

        return oauthAccountRepository.findFirstByUser_IdOrderByIdAsc(userId)
                .map(account -> account.getEmail())
                .orElse(null);
    }
}

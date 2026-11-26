package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.auth.entity.credential.LocalCredential;
import com.umc.bscene.domain.auth.repository.credential.LocalCredentialRepository;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.session.dto.profile.response.SessionBasicProfileResponse;
import com.umc.bscene.domain.session.dto.profile.response.SessionNameUpdateResponse;
import com.umc.bscene.domain.session.dto.profile.response.SessionProfileImageUpdateResponse;
import com.umc.bscene.domain.session.entity.SessionBasicProfile;
import com.umc.bscene.domain.session.enums.code.error.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionApplicationException;
import com.umc.bscene.domain.session.repository.SessionBasicProfileRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.media.MediaFileType;
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
    public SessionNameUpdateResponse updateName(Long userId, String name) {
        User user = getUser(userId);
        user.changeName(name.strip());
        return new SessionNameUpdateResponse(user.getName());
    }

    @Transactional
    public SessionProfileImageUpdateResponse updateProfileImage(Long userId, String profileImageUrl) {
        if (MediaFileType.isGifUrl(profileImageUrl)) {
            throw new SessionApplicationException(SessionErrorCode.INVALID_SESSION_PROFILE_IMAGE_REQUEST);
        }

        // 사용자별 프로필 최초 생성이 동시에 일어나지 않도록 사용자 행을 잠근다.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.UNAUTHORIZED_ERROR));
        SessionBasicProfile profile = sessionBasicProfileRepository.findByUser_Id(userId).orElse(null);
        String imageUrl = profileImageUrl.isEmpty() ? null : profileImageUrl;
        if (profile != null) {
            profile.changeProfileImage(imageUrl);
        } else if (imageUrl != null) {
            sessionBasicProfileRepository.save(SessionBasicProfile.builder()
                    .user(user)
                    .profileImageUrl(imageUrl)
                    .build());
        }
        return new SessionProfileImageUpdateResponse(imageUrl);
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

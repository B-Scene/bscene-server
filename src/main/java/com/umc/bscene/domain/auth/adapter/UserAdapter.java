package com.umc.bscene.domain.auth.adapter;

import com.umc.bscene.domain.auth.enums.code.AuthErrorCode;
import com.umc.bscene.domain.auth.exception.auth.AuthException;
import com.umc.bscene.domain.auth.repository.credential.LocalCredentialRepository;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.user.port.AuthPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserAdapter implements AuthPort {

    private final LocalCredentialRepository localCredentialRepository;
    private final OauthAccountRepository oauthAccountRepository;

    @Override
    public String getEmailToLocalCredential(Long userId) {
        return localCredentialRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND))
                .getLoginId();
    }

    @Override
    public String getEmailToOauthAccount(Long userId) {
        return oauthAccountRepository
                .findFirstByUser_IdOrderByIdAsc(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND))
                .getEmail();
    }
}

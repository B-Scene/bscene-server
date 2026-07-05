package com.umc.bscene.domain.auth.phoneverification.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneVerificationSendResponse {

    private Long expiresInSeconds;
}

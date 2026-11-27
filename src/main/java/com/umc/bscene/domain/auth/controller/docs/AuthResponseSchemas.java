package com.umc.bscene.domain.auth.controller.docs;

import com.umc.bscene.domain.auth.dto.auth.response.ReissueResponse;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.oauth.dto.response.AppleChallengeResponse;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.response.code.BaseResponseCode;

/** OpenAPI에서 공통 응답의 제네릭 결과 타입을 구체화한다. */
public final class AuthResponseSchemas {

    private AuthResponseSchemas() {
    }

    public abstract static class LoginOrSignupEnvelope extends SuccessResponse<TokenResponse> {
        protected LoginOrSignupEnvelope(TokenResponse result, BaseResponseCode code) {
            super(result, code);
        }
    }

    public abstract static class LogoutEnvelope extends SuccessResponse<Void> {
        protected LogoutEnvelope(BaseResponseCode code) {
            super(null, code);
        }
    }

    public abstract static class ReissueEnvelope extends SuccessResponse<ReissueResponse> {
        protected ReissueEnvelope(ReissueResponse result, BaseResponseCode code) {
            super(result, code);
        }
    }

    public abstract static class OauthLoginEnvelope extends SuccessResponse<OauthLoginResponse> {
        protected OauthLoginEnvelope(OauthLoginResponse result, BaseResponseCode code) {
            super(result, code);
        }
    }

    public abstract static class AppleChallengeEnvelope extends SuccessResponse<AppleChallengeResponse> {
        protected AppleChallengeEnvelope(AppleChallengeResponse result, BaseResponseCode code) {
            super(result, code);
        }
    }
}

package com.umc.bscene.global.swagger;

import com.umc.bscene.domain.chat.dto.response.ChatRoomCreateResponse;
import com.umc.bscene.domain.chat.dto.response.ChatRoomDetailResponse;
import com.umc.bscene.domain.chat.dto.response.ChatRoomListResponse;
import com.umc.bscene.domain.session.dto.application.response.MyApplicationSubmissionListResponse;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationSummaryResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSearchResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSubmitResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationVisibilityResponse;
import com.umc.bscene.domain.session.dto.profile.response.SessionBasicProfileResponse;
import com.umc.bscene.domain.session.dto.profile.response.SessionNameUpdateResponse;
import com.umc.bscene.domain.session.dto.profile.response.SessionProfileImageUpdateResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.InterestedRecruitmentListResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.RecentRecruitmentListResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.RecruitmentSearchKeywordResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentDetailResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentInterestResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListResponse;

import java.util.List;

/** 세션 문서에서 공통 응답의 result 타입을 구체화한다. */
public final class SessionApiResponseSchemas {

    private SessionApiResponseSchemas() {
    }

    public abstract static class ChatRoomCreateResponseEnvelope
            extends SwaggerResponseEnvelope<ChatRoomCreateResponse> {
    }

    public abstract static class ChatRoomDetailResponseEnvelope
            extends SwaggerResponseEnvelope<ChatRoomDetailResponse> {
    }

    public abstract static class ChatRoomListResponseEnvelope
            extends SwaggerResponseEnvelope<ChatRoomListResponse> {
    }

    public abstract static class InterestedRecruitmentListResponseEnvelope
            extends SwaggerResponseEnvelope<InterestedRecruitmentListResponse> {
    }

    public abstract static class RecruitmentSearchKeywordListEnvelope
            extends SwaggerResponseEnvelope<List<RecruitmentSearchKeywordResponse>> {
    }

    public abstract static class MyApplicationSubmissionListResponseEnvelope
            extends SwaggerResponseEnvelope<MyApplicationSubmissionListResponse> {
    }

    public abstract static class MySessionApplicationDetailResponseEnvelope
            extends SwaggerResponseEnvelope<MySessionApplicationDetailResponse> {
    }

    public abstract static class MySessionApplicationResponseEnvelope
            extends SwaggerResponseEnvelope<MySessionApplicationResponse> {
    }

    public abstract static class MySessionApplicationSummaryResponseEnvelope
            extends SwaggerResponseEnvelope<MySessionApplicationSummaryResponse> {
    }

    public abstract static class RecentRecruitmentListResponseEnvelope
            extends SwaggerResponseEnvelope<RecentRecruitmentListResponse> {
    }

    public abstract static class SessionApplicationDetailResponseEnvelope
            extends SwaggerResponseEnvelope<SessionApplicationDetailResponse> {
    }

    public abstract static class SessionApplicationSearchResponseEnvelope
            extends SwaggerResponseEnvelope<SessionApplicationSearchResponse> {
    }

    public abstract static class SessionApplicationSubmitResponseEnvelope
            extends SwaggerResponseEnvelope<SessionApplicationSubmitResponse> {
    }

    public abstract static class SessionApplicationVisibilityResponseEnvelope
            extends SwaggerResponseEnvelope<SessionApplicationVisibilityResponse> {
    }

    public abstract static class SessionBasicProfileResponseEnvelope
            extends SwaggerResponseEnvelope<SessionBasicProfileResponse> {
    }

    public abstract static class SessionNameUpdateResponseEnvelope
            extends SwaggerResponseEnvelope<SessionNameUpdateResponse> {
    }

    public abstract static class SessionProfileImageUpdateResponseEnvelope
            extends SwaggerResponseEnvelope<SessionProfileImageUpdateResponse> {
    }

    public abstract static class SessionRecruitmentDetailResponseEnvelope
            extends SwaggerResponseEnvelope<SessionRecruitmentDetailResponse> {
    }

    public abstract static class SessionRecruitmentInterestResponseEnvelope
            extends SwaggerResponseEnvelope<SessionRecruitmentInterestResponse> {
    }

    public abstract static class SessionRecruitmentListResponseEnvelope
            extends SwaggerResponseEnvelope<SessionRecruitmentListResponse> {
    }
}

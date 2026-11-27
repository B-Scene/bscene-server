package com.umc.bscene.global.swagger;

import com.umc.bscene.domain.auth.dto.onboarding.response.FanNicknameCheckResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.GenreResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.OnboardingStatusResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.RegionResponse;
import com.umc.bscene.domain.session.dto.application.response.SubmittedApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.ManagedRecruitmentListResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentCreateResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentEditResponse;
import com.umc.bscene.domain.user.dto.response.FollowedBandResponse;
import com.umc.bscene.domain.user.dto.response.InterestedPerformanceResponse;
import com.umc.bscene.domain.user.dto.response.MyInfoResponse;
import com.umc.bscene.domain.user.dto.response.MyProfileResponse;
import com.umc.bscene.domain.user.dto.response.ParticipationHistoryResponse;
import com.umc.bscene.domain.user.dto.response.SessionApplyConfirmResponse;
import com.umc.bscene.domain.user.dto.response.mypage.BandMyPageResponse;
import com.umc.bscene.domain.user.dto.response.mypage.FanMyPageResponse;
import com.umc.bscene.domain.user.dto.response.mypage.MyPageResponse;
import com.umc.bscene.domain.user.dto.response.session.SessionRecruitmentResponse;
import com.umc.bscene.global.response.CursorPage;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/** 문서에서 공통 응답의 result 타입을 구체화한다. */
public final class MyApiResponseSchemas {

    private MyApiResponseSchemas() {
    }

    public abstract static class OnboardingStatusResponseEnvelope extends SwaggerResponseEnvelope<OnboardingStatusResponse> {
    }

    public abstract static class FanNicknameCheckResponseEnvelope extends SwaggerResponseEnvelope<FanNicknameCheckResponse> {
    }

    public abstract static class GenreListEnvelope extends SwaggerResponseEnvelope<List<GenreResponse>> {
    }

    public abstract static class RegionListEnvelope extends SwaggerResponseEnvelope<List<RegionResponse>> {
    }

    public abstract static class MyProfileResponseEnvelope extends SwaggerResponseEnvelope<MyProfileResponse> {
    }

    public abstract static class EmptyEnvelope extends SwaggerResponseEnvelope<Void> {
    }

    public abstract static class MyPageResponseEnvelope extends SwaggerResponseEnvelope<MyPageResponse> {
        @Override
        @Schema(oneOf = {FanMyPageResponse.class, BandMyPageResponse.class})
        public abstract MyPageResponse getResult();
    }

    public abstract static class ParticipationHistoryResponseEnvelope extends SwaggerResponseEnvelope<ParticipationHistoryResponse> {
    }

    public abstract static class InterestedPerformanceResponseEnvelope extends SwaggerResponseEnvelope<InterestedPerformanceResponse> {
    }

    public abstract static class FollowedBandResponseEnvelope extends SwaggerResponseEnvelope<FollowedBandResponse> {
    }

    public abstract static class MyInfoResponseEnvelope extends SwaggerResponseEnvelope<MyInfoResponse> {
    }

    public abstract static class ReceivedRecruitmentsEnvelope extends SwaggerResponseEnvelope<CursorPage<SessionRecruitmentResponse>> {
    }

    public abstract static class SessionApplyConfirmResponseEnvelope extends SwaggerResponseEnvelope<SessionApplyConfirmResponse> {
    }

    public abstract static class SubmittedApplicationDetailResponseEnvelope extends SwaggerResponseEnvelope<SubmittedApplicationDetailResponse> {
    }

    public abstract static class ManagedRecruitmentListResponseEnvelope extends SwaggerResponseEnvelope<ManagedRecruitmentListResponse> {
    }

    public abstract static class SessionRecruitmentEditResponseEnvelope extends SwaggerResponseEnvelope<SessionRecruitmentEditResponse> {
    }

    public abstract static class SessionRecruitmentCreateResponseEnvelope extends SwaggerResponseEnvelope<SessionRecruitmentCreateResponse> {
    }
}

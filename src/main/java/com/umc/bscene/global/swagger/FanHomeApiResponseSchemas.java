package com.umc.bscene.global.swagger;

import com.umc.bscene.domain.band.dto.response.BandPublicMemberProfileResponse;
import com.umc.bscene.domain.fanhome.dto.response.DatePerformanceResponse;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse;
import com.umc.bscene.domain.fanhome.dto.response.FollowingBandNewsResponse;
import com.umc.bscene.domain.fanhome.dto.response.PerformanceCalendarResponse;
import com.umc.bscene.domain.fanhome.dto.response.RecommendedPerformanceResponse;
import com.umc.bscene.domain.fanhome.dto.response.UpcomingPerformanceResponse;
import com.umc.bscene.domain.performance.dto.response.PendingParticipationResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceAlarmResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceDetailResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceInterestResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceParticipationDeclineResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceParticipationResponse;

import java.util.List;

/** 팬모드 홈의 공통 응답에서 실제 result 타입을 표시한다. */
public final class FanHomeApiResponseSchemas {

    private FanHomeApiResponseSchemas() {
    }

    public abstract static class DatePerformanceResponseEnvelope
            extends SwaggerResponseEnvelope<DatePerformanceResponse> {
    }

    public abstract static class FanHomeResponseEnvelope
            extends SwaggerResponseEnvelope<FanHomeResponse> {
    }

    public abstract static class FollowingBandNewsResponseEnvelope
            extends SwaggerResponseEnvelope<FollowingBandNewsResponse> {
    }

    public abstract static class BandPublicMemberProfileListEnvelope
            extends SwaggerResponseEnvelope<List<BandPublicMemberProfileResponse>> {
    }

    public abstract static class PendingParticipationResponseEnvelope
            extends SwaggerResponseEnvelope<PendingParticipationResponse> {
    }

    public abstract static class PerformanceAlarmResponseEnvelope
            extends SwaggerResponseEnvelope<PerformanceAlarmResponse> {
    }

    public abstract static class PerformanceCalendarResponseEnvelope
            extends SwaggerResponseEnvelope<PerformanceCalendarResponse> {
    }

    public abstract static class PerformanceDetailResponseEnvelope
            extends SwaggerResponseEnvelope<PerformanceDetailResponse> {
    }

    public abstract static class PerformanceInterestResponseEnvelope
            extends SwaggerResponseEnvelope<PerformanceInterestResponse> {
    }

    public abstract static class PerformanceParticipationDeclineResponseEnvelope
            extends SwaggerResponseEnvelope<PerformanceParticipationDeclineResponse> {
    }

    public abstract static class PerformanceParticipationResponseEnvelope
            extends SwaggerResponseEnvelope<PerformanceParticipationResponse> {
    }

    public abstract static class RecommendedPerformanceResponseEnvelope
            extends SwaggerResponseEnvelope<RecommendedPerformanceResponse> {
    }

    public abstract static class UpcomingPerformanceResponseEnvelope
            extends SwaggerResponseEnvelope<UpcomingPerformanceResponse> {
    }
}

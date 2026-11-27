package com.umc.bscene.global.swagger;

import com.umc.bscene.domain.stream.dto.response.BandLiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.FanLiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.LiveAlarmToggleResponse;
import com.umc.bscene.domain.stream.dto.response.LiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.LiveMembersResponse;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.ReplayResponse;
import com.umc.bscene.domain.stream.dto.response.ReportUserResponse;
import com.umc.bscene.domain.stream.dto.response.ReservationEditResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;
import com.umc.bscene.domain.stream.dto.response.StreamReplayResponse;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.dto.response.StreamSummaryResponse;
import com.umc.bscene.domain.stream.dto.response.UpcomingLiveResponse;
import com.umc.bscene.global.response.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;

/** 라이브 문서에서 공통 응답의 실제 데이터 타입을 표시한다. */
public final class FanLiveApiResponseSchemas {

    private FanLiveApiResponseSchemas() {
    }

    public abstract static class LiveStreamResponsePageEnvelope extends SwaggerResponseEnvelope<CursorPage<LiveStreamResponse>> {
    }

    public abstract static class ReplayResponsePageEnvelope extends SwaggerResponseEnvelope<CursorPage<ReplayResponse>> {
    }

    public abstract static class UpcomingLiveResponsePageEnvelope extends SwaggerResponseEnvelope<CursorPage<UpcomingLiveResponse>> {
    }

    public abstract static class LiveAlarmToggleResponseEnvelope extends SwaggerResponseEnvelope<LiveAlarmToggleResponse> {
    }

    public abstract static class LiveHomeResponseEnvelope extends SwaggerResponseEnvelope<LiveHomeResponse> {
        @Override
        @Schema(oneOf = {FanLiveHomeResponse.class, BandLiveHomeResponse.class})
        public abstract LiveHomeResponse getResult();
    }

    public abstract static class ReportUserResponseEnvelope extends SwaggerResponseEnvelope<ReportUserResponse> {
    }

    public abstract static class StreamReplayResponseEnvelope extends SwaggerResponseEnvelope<StreamReplayResponse> {
    }

    public abstract static class StreamRoomResponseEnvelope extends SwaggerResponseEnvelope<StreamRoomResponse> {
    }

    public abstract static class LiveMembersResponseEnvelope extends SwaggerResponseEnvelope<LiveMembersResponse> {
    }

    public abstract static class ReservationEditResponseEnvelope extends SwaggerResponseEnvelope<ReservationEditResponse> {
    }

    public abstract static class StreamCreateResponseEnvelope extends SwaggerResponseEnvelope<StreamCreateResponse> {
    }

    public abstract static class StreamSummaryResponseEnvelope extends SwaggerResponseEnvelope<StreamSummaryResponse> {
    }
}

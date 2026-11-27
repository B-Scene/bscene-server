package com.umc.bscene.global.swagger;

import com.umc.bscene.domain.band.dto.response.BandDetailResponse;
import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;
import com.umc.bscene.domain.follow.dto.response.FollowResponse;
import com.umc.bscene.domain.post.dto.response.PostCommentListResponse;
import com.umc.bscene.domain.post.dto.response.PostCommentResponse;
import com.umc.bscene.domain.post.dto.response.PostDetailResponse;
import com.umc.bscene.domain.post.dto.response.PostLikeResponse;
import com.umc.bscene.domain.search.dto.response.ExploreSearchResponse;
import com.umc.bscene.domain.search.dto.response.RecentSearchListResponse;

/** 팬모드 탐색의 응답 데이터 타입을 Swagger에 표시한다. */
public final class ExploreApiResponseSchemas {

    private ExploreApiResponseSchemas() {
    }

    public abstract static class BandDetailResponseEnvelope extends SwaggerResponseEnvelope<BandDetailResponse> {
    }

    public abstract static class BandRecommendResponseEnvelope extends SwaggerResponseEnvelope<BandRecommendResponse> {
    }

    public abstract static class ExploreSearchResponseEnvelope extends SwaggerResponseEnvelope<ExploreSearchResponse> {
    }

    public abstract static class FollowResponseEnvelope extends SwaggerResponseEnvelope<FollowResponse> {
    }

    public abstract static class PostCommentListResponseEnvelope extends SwaggerResponseEnvelope<PostCommentListResponse> {
    }

    public abstract static class PostCommentResponseEnvelope extends SwaggerResponseEnvelope<PostCommentResponse> {
    }

    public abstract static class PostDetailResponseEnvelope extends SwaggerResponseEnvelope<PostDetailResponse> {
    }

    public abstract static class PostLikeResponseEnvelope extends SwaggerResponseEnvelope<PostLikeResponse> {
    }

    public abstract static class RecentSearchListResponseEnvelope extends SwaggerResponseEnvelope<RecentSearchListResponse> {
    }
}

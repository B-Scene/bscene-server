package com.umc.bscene.global.swagger;

import com.umc.bscene.domain.band.dto.response.MusicLinkResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceListResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceResponse;
import com.umc.bscene.domain.post.dto.response.PostCreateResponse;
import com.umc.bscene.domain.post.dto.response.PostListResponse;
import com.umc.bscene.domain.post.dto.response.PostResponse;
import com.umc.bscene.domain.post.dto.response.PostUpdateResponse;

public final class BandContentResponseSchemas {

    private BandContentResponseSchemas() {
    }

    public abstract static class PostCreationEnvelope extends SwaggerResponseEnvelope<PostCreateResponse> {
    }

    public abstract static class PostListEnvelope extends SwaggerResponseEnvelope<PostListResponse> {
    }

    public abstract static class PostDetailEnvelope extends SwaggerResponseEnvelope<PostResponse> {
    }

    public abstract static class PostUpdateEnvelope extends SwaggerResponseEnvelope<PostUpdateResponse> {
    }

    public abstract static class PerformanceEnvelope extends SwaggerResponseEnvelope<PerformanceResponse> {
    }

    public abstract static class PerformanceListEnvelope extends SwaggerResponseEnvelope<PerformanceListResponse> {
    }

    public abstract static class MusicLinkEnvelope extends SwaggerResponseEnvelope<MusicLinkResponse> {
    }
}

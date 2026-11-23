package com.umc.bscene.domain.post.service;

import com.umc.bscene.domain.post.event.PostVideoThumbnailRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostThumbnailServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private PostService postService;

    @InjectMocks
    private PostThumbnailService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "bucket", "bscene-bucket");
        ReflectionTestUtils.setField(service, "region", "ap-northeast-2");
    }

    // 우리 버킷 호스트가 아니거나, 게시물 미디어 접두사 밖이거나, 영상 확장자가 아니면 S3에 접근조차 하지 않아야 한다
    @ParameterizedTest
    @ValueSource(strings = {
            "https://attacker-bucket.s3.ap-northeast-2.amazonaws.com/post/a.mp4",
            "http://bscene-bucket.s3.ap-northeast-2.amazonaws.com/post/a.mp4",
            "https://bscene-bucket.s3.ap-northeast-2.amazonaws.com/user_profile/secret.mp4",
            "https://bscene-bucket.s3.ap-northeast-2.amazonaws.com/recordings/live-1/segment.mp4",
            "https://bscene-bucket.s3.ap-northeast-2.amazonaws.com/post/../user_profile/x.mp4",
            "https://bscene-bucket.s3.ap-northeast-2.amazonaws.com/post/a.jpg",
            "https://bscene-bucket.s3.ap-northeast-2.amazonaws.com/post/a.mp4?x=1",
            "https://bscene-bucket.s3.ap-northeast-2.amazonaws.com/",
            "not a url at all",
            "/post/a.mp4"
    })
    void 허용되지_않은_영상_URL은_S3에_접근하지_않는다(String videoUrl) {
        service.handle(new PostVideoThumbnailRequestedEvent(1L, videoUrl));

        verifyNoInteractions(s3Client);
        verify(postService, never()).applyGeneratedThumbnail(any(), any());
    }

    @Test
    void 허용_범위를_넘는_크기의_영상은_내려받지_않는다() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(600L * 1024 * 1024).build());

        service.handle(new PostVideoThumbnailRequestedEvent(
                1L, "https://bscene-bucket.s3.ap-northeast-2.amazonaws.com/post/a.mp4"));

        verify(s3Client).headObject(any(HeadObjectRequest.class));
        verify(s3Client, never()).getObject(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class),
                any(java.nio.file.Path.class));
        verify(postService, never()).applyGeneratedThumbnail(any(), any());
    }
}

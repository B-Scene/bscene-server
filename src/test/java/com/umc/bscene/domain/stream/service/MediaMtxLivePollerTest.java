package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.response.MtxPathListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * MediaMTX 폴링 검증.
 * <p>
 * 핵심 불변식:
 * - ready=true인 path만 라이브 상태 동기화 대상이다
 * - MediaMTX가 죽어도 폴링 1회를 건너뛸 뿐 예외를 전파하지 않으며, 이때 동기화를 호출해선 안 된다
 *   (빈 집합을 넘기면 살아 있는 라이브 키가 전부 지워진다)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MediaMtxLivePoller")
class MediaMtxLivePollerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient mtxRestClient;

    @Mock
    private StreamService streamService;

    @Captor
    private ArgumentCaptor<Set<String>> readyPathsCaptor;

    private MediaMtxLivePoller poller;

    @BeforeEach
    void setUp() {
        poller = new MediaMtxLivePoller(mtxRestClient, streamService);
    }

    private void givenPathList(MtxPathListResponse response) {
        when(mtxRestClient.get()
                .uri("v3/paths/list")
                .retrieve()
                .body(MtxPathListResponse.class))
                .thenReturn(response);
    }

    private MtxPathListResponse pathList(MtxPathListResponse.Item... items) {
        List<MtxPathListResponse.Item> list = List.of(items);
        return new MtxPathListResponse(list.size(), 1, list);
    }

    @Nested
    @DisplayName("poll()")
    class Poll {

        @Test
        @DisplayName("ready=true인 path의 이름만 동기화 대상으로 넘긴다")
        void passesOnlyReadyPathNames() {
            givenPathList(pathList(
                    new MtxPathListResponse.Item("live-a", true),
                    new MtxPathListResponse.Item("live-b", false),
                    new MtxPathListResponse.Item("live-c", true)
            ));

            poller.poll();

            verify(streamService).syncLiveState(readyPathsCaptor.capture());
            assertThat(readyPathsCaptor.getValue()).containsExactlyInAnyOrder("live-a", "live-c");
        }

        @Test
        @DisplayName("모든 path가 ready=false면 빈 집합을 넘긴다")
        void passesEmptySetWhenNothingReady() {
            givenPathList(pathList(
                    new MtxPathListResponse.Item("live-a", false),
                    new MtxPathListResponse.Item("live-b", false)
            ));

            poller.poll();

            verify(streamService).syncLiveState(readyPathsCaptor.capture());
            assertThat(readyPathsCaptor.getValue()).isEmpty();
        }

        @Test
        @DisplayName("path 목록 자체가 비어 있어도 빈 집합을 넘긴다")
        void passesEmptySetWhenNoPaths() {
            givenPathList(pathList());

            poller.poll();

            verify(streamService).syncLiveState(readyPathsCaptor.capture());
            assertThat(readyPathsCaptor.getValue()).isEmpty();
        }

        @Test
        @DisplayName("ready=true가 하나뿐이면 그 이름만 넘긴다")
        void passesSingleReadyPath() {
            givenPathList(pathList(new MtxPathListResponse.Item("only-live", true)));

            poller.poll();

            verify(streamService).syncLiveState(readyPathsCaptor.capture());
            assertThat(readyPathsCaptor.getValue()).containsExactly("only-live");
        }

        @Test
        @DisplayName("MediaMTX 호출이 실패하면 예외를 삼키고 동기화를 호출하지 않는다")
        void swallowsRestClientExceptionWithoutSyncing() {
            when(mtxRestClient.get()
                    .uri("v3/paths/list")
                    .retrieve()
                    .body(MtxPathListResponse.class))
                    .thenThrow(new RestClientException("MediaMTX 다운"));

            assertThatCode(() -> poller.poll()).doesNotThrowAnyException();

            verifyNoInteractions(streamService);
        }
    }
}

package com.umc.bscene.support;

import com.umc.bscene.domain.stream.dto.CoHostCandidateInfo;
import com.umc.bscene.domain.stream.dto.ReplayDurationSum;
import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.StreamReplay;
import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import com.umc.bscene.domain.stream.enums.StreamMemberStatus;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.enums.UserMode;
import org.springframework.data.redis.core.Cursor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

/**
 * stream 도메인 단위 테스트용 공용 픽스처.
 * 엔티티들이 클래스 레벨 @Builder + 전체 인자 생성자를 쓰므로 id까지 빌더로 지정 가능하다.
 */
public final class StreamFixtures {

    private StreamFixtures() {
    }

    public static User user(Long id, UserMode mode) {
        return User.builder()
                .id(id)
                .name("user" + id)
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .phone("0100000" + String.format("%04d", id % 10000))
                .currentMode(mode)
                .build();
    }

    public static User bandUser(Long id) {
        return user(id, UserMode.BAND);
    }

    public static User fanUser(Long id) {
        return user(id, UserMode.FAN);
    }

    /** 기본 OPEN 라이브. path는 "path-{id}". */
    public static AudioStream stream(Long id, Long broadcasterId, Long bandId, StreamStatus status) {
        return AudioStream.builder()
                .id(id)
                .broadcasterId(broadcasterId)
                .bandId(bandId)
                .path("path-" + id)
                .title("title-" + id)
                .description("description-" + id)
                .thumbnailImageUrl("https://cdn.test/thumb-" + id + ".jpg")
                .status(status)
                .build();
    }

    public static AudioStream scheduledStream(Long id, Long broadcasterId, Long bandId, LocalDateTime scheduledAt) {
        return AudioStream.builder()
                .id(id)
                .broadcasterId(broadcasterId)
                .bandId(bandId)
                .path("path-" + id)
                .title("title-" + id)
                .description("description-" + id)
                .thumbnailImageUrl("https://cdn.test/thumb-" + id + ".jpg")
                .status(StreamStatus.SCHEDULED)
                .scheduledAt(scheduledAt)
                .build();
    }

    public static AudioStream closedStream(
            Long id, Long broadcasterId, Long bandId,
            LocalDateTime startedAt, LocalDateTime closedAt, Integer closedViewerCount
    ) {
        return AudioStream.builder()
                .id(id)
                .broadcasterId(broadcasterId)
                .bandId(bandId)
                .path("path-" + id)
                .title("title-" + id)
                .status(StreamStatus.CLOSED)
                .startedAt(startedAt)
                .closedAt(closedAt)
                .closedViewerCount(closedViewerCount)
                .build();
    }

    public static StreamReplay replay(Long id, AudioStream audioStream, String s3Key, int durationSec, long viewCount) {
        return StreamReplay.builder()
                .id(id)
                .audioStream(audioStream)
                .s3Key(s3Key)
                .durationSec(durationSec)
                .viewCount(viewCount)
                .build();
    }

    public static StreamMember member(Long id, User user, AudioStream audioStream, StreamMemberStatus status) {
        return StreamMember.builder()
                .id(id)
                .user(user)
                .audioStream(audioStream)
                .status(status)
                .build();
    }

    public static BandInfoForGetLiveResponse bandInfo(Long broadcasterId, String bandName, String profileImageUrl) {
        return new BandInfoForGetLiveResponse(broadcasterId, bandName, profileImageUrl);
    }

    public static CoHostCandidateInfo candidate(Long userId, Long bandMemberId) {
        return new CoHostCandidateInfo(
                userId, bandMemberId, bandMemberId, "https://cdn.test/p" + userId + ".jpg", "nick" + userId, "기타"
        );
    }

    public static ReplayDurationSum durationSum(Long audioStreamId, long totalDurationSec) {
        return new ReplayDurationSum(audioStreamId, totalDurationSec);
    }

    /**
     * StringRedisTemplate#scan이 반환할 Cursor.
     * <p>
     * Mockito mock이 아니라 실제 구현이다. {@code when(...).thenReturn(redisCursor(...))} 형태로 쓰면
     * 진행 중인 스터빙 도중에 새 mock을 만들게 되어 UnfinishedStubbingException이 나기 때문.
     * 닫혔는지는 {@link FakeCursor#isClosed()}로 확인한다.
     */
    public static FakeCursor redisCursor(String... keys) {
        return new FakeCursor(List.of(keys));
    }

    /** 스캔 결과를 순회하고 close 여부를 노출하는 Cursor 구현. */
    public static final class FakeCursor implements Cursor<String> {

        private final Iterator<String> iterator;
        private long position;
        private boolean closed;

        private FakeCursor(List<String> keys) {
            this.iterator = keys.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public String next() {
            position++;
            return iterator.next();
        }

        @Override
        public void close() {
            this.closed = true;
        }

        @Override
        public CursorId getId() {
            return CursorId.initial();
        }

        @Override
        public long getCursorId() {
            return 0L;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public long getPosition() {
            return position;
        }
    }
}

package com.umc.bscene.domain.stream.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AudioStream extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audio_stream_id")
    private Long id;

    // userId이지만, 제약 조건은 명시하지 X. 애플리케이션 단에서 정합성 보장
    @Column(name = "broadcaster_id", nullable = false)
    private Long broadcasterId;

    // 라이브의 주체인 밴드를 생성 시점에 직접 연결한다.
    @Column(name = "band_id", nullable = false)
    private Long bandId;

    @Column(nullable = false, unique = true)
    private String path;

    @Column(nullable = false)
    private String title;

    @Column(length = 100)
    private String description;

    private String thumbnailImageUrl;

    // MySQL 이외의 DBMS는 enum 타입 미지원 가능성 존재. 따라서 Enumurated STRING으로 문자열로 관리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StreamStatus status;

    private LocalDateTime scheduledAt;

    private LocalDateTime startedAt;

    private LocalDateTime closedAt;

    // 방송 종료 시점의 시청자 수 스냅샷. 종료 전에는 null
    private Integer closedViewerCount;

    // 밴드 구성원 대상 라이브 시작 30분 전 알림 발송 시각
    @Column(name = "member_reminder_sent_at")
    private LocalDateTime memberReminderSentAt;

    // 모든 녹화 세그먼트의 다시보기 등록 완료 알림 발송 시각
    @Column(name = "replay_notification_sent_at")
    private LocalDateTime replayNotificationSentAt;

    @JsonManagedReference
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "audioStream")
    private List<StreamMember> coHost;

    public void close(int closedViewerCount) {
        this.status = StreamStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
        this.closedViewerCount = closedViewerCount;
    }

    public void markStarted() {
        if(this.startedAt == null)
            this.startedAt = LocalDateTime.now();

        this.status = StreamStatus.OPEN;
    }

    public void cancel() {
        // 이미 시작(OPEN)되었거나 종료된 방을 예약 취소로 덮지 않도록 방어
        if(this.status != StreamStatus.SCHEDULED)
            return;

        this.status = StreamStatus.CANCELED;
        this.closedAt = LocalDateTime.now();
    }

    public void markMemberReminderSent(LocalDateTime sentAt) {
        this.memberReminderSentAt = sentAt;
    }
}

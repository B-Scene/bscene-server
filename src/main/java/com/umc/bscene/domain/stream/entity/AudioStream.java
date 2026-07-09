package com.umc.bscene.domain.stream.entity;

import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Stream;

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

    public void close() {
        this.status = StreamStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public void markStarted() {
        if(this.startedAt == null)
            this.startedAt = LocalDateTime.now();

        this.status = StreamStatus.OPEN;
    }
}

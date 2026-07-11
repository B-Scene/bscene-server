package com.umc.bscene.domain.stream.entity;

import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StreamReplay extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stream_replay_id")
    private Long id;

    // StreamReplay N:1 AudioStream. 3시간 초과 라이브는 세그먼트 파일이 여러 개로 나뉘어 여러 행이 될 수 있음(보통 1개).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_stream_id", nullable = false)
    private AudioStream audioStream;

    // unique 제약으로 동일 파일에 대한 훅/스위퍼의 동시·중복 저장이 충돌하여 중복 행이 생기지 않도록 보장(경합 안전)
    @Column(nullable = false, unique = true)
    private String s3Key;

    // ffprobe로 추출한 재생 길이(초)
    @Column(nullable = false)
    private Integer durationSec;

    // 다시보기 재생 횟수. 0에서 시작
    @Column(nullable = false)
    private long viewCount;

    public void increaseViewCount() {
        this.viewCount++;
    }
}

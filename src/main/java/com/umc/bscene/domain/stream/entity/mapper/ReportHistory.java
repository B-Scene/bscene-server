package com.umc.bscene.domain.stream.entity.mapper;

import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.enums.ReportType;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 라이브 내 유저 신고 이력. 언제(BaseEntity.createdAt) 어느 라이브에서 어떤 유저가
 * 어떤 사유로 신고당했는지, 어떤 채팅을 쳤었는지를 보관한다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReportHistory extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신고당한 유저
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User targetUser;

    // 신고가 발생한 라이브
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_stream_id", nullable = false)
    private AudioStream audioStream;

    // 신고자. 관계 탐색이 필요 없으므로 FK 제약 없이 ID만 보관 (AudioStream.broadcasterId와 동일한 방식)
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    // 신고 대상이 작성한 문제 채팅 내용. 채팅은 서버에 저장되지 않으므로 신고 시점 스냅샷으로 보관
    @Column(length = 500, nullable = false)
    private String chatMessage;

    // 신고자가 작성한 상세 코멘트
    @Column(length = 500)
    private String comment;
}

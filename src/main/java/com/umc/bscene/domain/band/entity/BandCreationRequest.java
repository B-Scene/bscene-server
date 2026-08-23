package com.umc.bscene.domain.band.entity;

import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 밴드 생성 검수 요청.
 * 검수 상태 자체는 Band.status(PENDING/ACCEPTED)에 있고,
 * 이 테이블은 요청자·Discord 메시지·처리 결과의 감사 이력을 담는다.
 * 거절 시 Band row는 삭제되므로 band FK는 nullable, 밴드명은 스냅샷으로 보존한다.
 */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "BandCreateRequest")
public class BandCreationRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandId")
    private Band band;

    @Column(nullable = false)
    private Long requesterId;

    // 거절로 Band가 삭제된 뒤에도 이력 조회가 가능하도록 스냅샷 보관
    @Column(nullable = false, length = 100)
    private String bandName;

    // 검수 메시지 재전송/추적용. 전송 실패 시 null
    @Column(length = 64)
    private String discordMessageId;

    @Column(length = 200)
    private String rejectedReason;

    // 처리한 운영진의 Discord 태그
    @Column(length = 100)
    private String processedBy;

    // null이면 검수 진행중
    private LocalDateTime resolvedAt;

    public void attachDiscordMessage(String discordMessageId) {
        this.discordMessageId = discordMessageId;
    }

    public void markAccepted(String processedBy) {
        this.processedBy = processedBy;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markRejected(String rejectedReason, String processedBy) {
        this.rejectedReason = rejectedReason;
        this.processedBy = processedBy;
        this.resolvedAt = LocalDateTime.now();
        this.band = null;
    }
}

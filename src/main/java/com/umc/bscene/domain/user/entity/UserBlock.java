package com.umc.bscene.domain.user.entity;

import com.umc.bscene.global.entity.BaseEntity;
import com.umc.bscene.domain.stream.entity.AudioStream;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_blocks", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_blocks_live_blocker_blocked",
        columnNames = {"audio_stream_id", "blocker_id", "blocked_id"}))
public class UserBlock extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userBlockId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audio_stream_id", nullable = false)
    private AudioStream audioStream;

    @Builder
    private UserBlock(User blocker, User blocked, AudioStream audioStream) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.audioStream = audioStream;
    }
}

package com.umc.bscene.domain.stream.entity.mapper;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.enums.StreamMemberStatus;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        uniqueConstraints = {
                // 동시 공동 진행자 교체 요청이 같은 (user, stream) 조합을 중복 삽입하지 못하도록 방지
                @UniqueConstraint(name = "uk_stream_member_user_stream", columnNames = {"user_id", "audio_stream_id"})
        }
)
public class StreamMember extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stream_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 순환 참조 직렬화 방지: AudioStream.coHost가 @JsonManagedReference(부모), 이쪽이 @JsonBackReference(자식)
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_stream_id")
    private AudioStream audioStream;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StreamMemberStatus status;

    // 진행자 고유 path
    @Column(unique = true, length = 64)
    private String path;

    public void assignPath(String path) {
        // 재접속 시 동일 path를 재사용해야 MediaMTX overridePublisher가 좀비 세션을 정리한다
        if (this.path == null)
            this.path = path;
    }
}

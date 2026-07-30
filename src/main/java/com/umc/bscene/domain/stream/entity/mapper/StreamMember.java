package com.umc.bscene.domain.stream.entity.mapper;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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

    @JsonManagedReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_stream_id")
    private AudioStream audioStream;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StreamMemberStatus status;
}

package com.umc.bscene.domain.stream.entity;

import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AudioStream extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
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
}

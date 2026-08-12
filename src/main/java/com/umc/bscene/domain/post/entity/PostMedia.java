package com.umc.bscene.domain.post.entity;

import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PostMedia")
public class PostMedia extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 미디어가 속한 콘텐츠
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId", nullable = false)
    private Post post;

    @Column(nullable = false, length = 500)
    private String mediaUrl;

    // 노출 순서 (PHOTO 다중 이미지 정렬 기준)
    @Builder.Default
    @Column(nullable = false)
    private Integer sortOrder = 0;
}

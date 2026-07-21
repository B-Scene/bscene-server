package com.umc.bscene.domain.post.entity;

import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PostTag")
public class PostTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 태그가 속한 콘텐츠
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId", nullable = false)
    private Post post;

    @Column(nullable = false, length = 30)
    private String tagName;
}

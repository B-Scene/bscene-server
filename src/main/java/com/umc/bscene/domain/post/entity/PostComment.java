package com.umc.bscene.domain.post.entity;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PostComment")
public class PostComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글이 달린 콘텐츠
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId", nullable = false)
    private Post post;

    // 댓글 작성자 (화면 표시는 팬 프로필 닉네임 사용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String content;

    // 댓글 수정 (본인 댓글 검증은 서비스에서)
    public void updateContent(String content) {
        this.content = content;
    }
}

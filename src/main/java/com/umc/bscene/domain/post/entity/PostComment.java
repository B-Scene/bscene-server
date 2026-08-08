package com.umc.bscene.domain.post.entity;

import com.umc.bscene.domain.band.entity.BandMemberProfile;
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

    // 댓글 작성자 (팬 댓글의 화면 표시는 팬 프로필 닉네임 사용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    // 밴드모드로 작성한 댓글의 명의 — 작성 시점의 멤버 프로필을 박제. null이면 팬모드 댓글
    // (명의가 소멸하면(강퇴/탈퇴/프로필 삭제) 댓글도 함께 삭제된다 — band 도메인의 PostCommentPort 참조)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandMemberProfileId")
    private BandMemberProfile bandMemberProfile;

    @Column(nullable = false, length = 500)
    private String content;

    // 댓글 수정 (본인 댓글 검증은 서비스에서)
    public void updateContent(String content) {
        this.content = content;
    }
}

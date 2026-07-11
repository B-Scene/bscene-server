package com.umc.bscene.domain.post.entity;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.post.enums.PostType;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Post")
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 콘텐츠를 등록한 밴드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandId", nullable = false)
    private Band band;

    // 콘텐츠 종류 (등록 후 수정 불가 — 변경하려면 삭제 후 재등록)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PostType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    // 영상 콘텐츠의 이미지 썸네일 (사진/글 콘텐츠는 사용하지 않음)
    @Column(length = 500)
    private String thumbnailUrl;

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")
    private List<PostMedia> mediaList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTag> tagList = new ArrayList<>();

    // 수정 API에서 전달된 값만 부분 반영
    public void update(String title, String description, String thumbnailUrl) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (thumbnailUrl != null) this.thumbnailUrl = thumbnailUrl;
    }

    public void clearMedia() {
        this.mediaList.clear();
    }

    public void addMedia(String mediaUrl, int sortOrder) {
        this.mediaList.add(PostMedia.builder()
                .post(this)
                .mediaUrl(mediaUrl)
                .sortOrder(sortOrder)
                .build());
    }

    public void clearTags() {
        this.tagList.clear();
    }

    public void addTag(String tagName) {
        this.tagList.add(PostTag.builder()
                .post(this)
                .tagName(tagName)
                .build());
    }
}

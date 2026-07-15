package com.umc.bscene.domain.search.document;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.entity.PostTag;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 영상 게시물(Post, type=VIDEO) 검색용 문서.
 * MySQL이 원본(source of truth)이고 이 문서는 검색용 사본 — 밴드명·장르·지역을 비정규화해서 담는다.
 * 문서 ID = Post PK (재색인 시 같은 ID로 덮어써 멱등)
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "videos")
@Setting(settingPath = "elasticsearch/korean-settings.json")
public class VideoDocument {

    @Id
    private Long id;

    // 페이징 안정성용 tie-breaker (정렬 전용 — _id는 정렬 불가라 doc_values 필드로 복제)
    @Field(type = FieldType.Long, index = false)
    private Long docId;

    // 검색 대상 (가중치 : title^3) + 완전 일치 가점용 raw
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "korean"),
            otherFields = @InnerField(suffix = "raw", type = FieldType.Keyword)
    )
    private String title;

    // 검색 대상 (bandName^2) : 제목에 밴드명이 없어도 밴드명으로 검색되도록 비정규화
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "korean"),
            otherFields = @InnerField(suffix = "raw", type = FieldType.Keyword)
    )
    private String bandName;

    // 검색 대상 (tags^2) : 곡명 검색의 주 매칭 지점
    @Field(type = FieldType.Text, analyzer = "korean")
    private List<String> tags;

    // 검색 대상 (가중치 1, 최하위)
    @Field(type = FieldType.Text, analyzer = "korean")
    private String description;

    // 필터용 : 밴드의 장르·지역을 따름 (비정규화)
    @Field(type = FieldType.Keyword)
    private String genre;

    @Field(type = FieldType.Keyword)
    private String region;

    // 정렬(동점 시 최신순)·표시용
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime uploadedAt;

    // 표시용 (검색 대상 아님)
    @Field(type = FieldType.Keyword, index = false)
    private String thumbnailUrl;

    // 밴드 정보 변경 시 연쇄 재색인 대상 조회용
    @Field(type = FieldType.Long)
    private Long bandId;

    // band는 반드시 fetch join으로 함께 조회된 상태여야 한다 (LAZY 프록시 추가 쿼리 방지)
    public static VideoDocument from(Post post) {
        Band band = post.getBand();
        return VideoDocument.builder()
                .id(post.getId())
                .docId(post.getId())
                .title(post.getTitle())
                .bandName(band.getName())
                .tags(post.getTagList().stream().map(PostTag::getTagName).toList())
                .description(post.getDescription())
                .genre(band.getGenre().name())
                .region(band.getRegion().name())
                .uploadedAt(post.getCreatedAt())
                .thumbnailUrl(post.getThumbnailUrl())
                .bandId(band.getId())
                .build();
    }
}

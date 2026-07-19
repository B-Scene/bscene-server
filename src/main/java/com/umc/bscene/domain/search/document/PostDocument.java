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
 * 게시물(Post — 영상/사진/글) 검색용 문서.
 * MySQL이 원본(source of truth)이고 이 문서는 검색용 사본 — 밴드명·장르·지역을 비정규화해서 담는다.
 * 문서 ID = Post PK (재색인 시 같은 ID로 덮어써 멱등)
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// createIndex=false : 인덱스 생성은 SearchIndexService.recreateIndex()가 전담 (기동 시 ES 무접촉)
@Document(indexName = "posts", createIndex = false)
@Setting(settingPath = "elasticsearch/korean-settings.json")
public class PostDocument {

    @Id
    private Long id;

    // 페이징 안정성용 tie-breaker (정렬 전용 — _id는 정렬 불가라 doc_values 필드로 복제)
    @Field(type = FieldType.Long, index = false)
    private Long docId;

    // 게시물 종류 (VIDEO/PHOTO/TEXT) : 카드 표시용 + 향후 종류별 하위 필터 대비 keyword 색인
    @Field(type = FieldType.Keyword)
    private String postType;

    // 검색 대상 (가중치 : title^3) + 완전 일치 가점용 raw + 접두어(부분 입력) 검색용 prefix
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "korean", searchAnalyzer = "korean_search"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword),
                    @InnerField(suffix = "prefix", type = FieldType.Text,
                            analyzer = "korean_prefix", searchAnalyzer = "korean")
            }
    )
    private String title;

    // 검색 대상 (bandName^2) : 제목에 밴드명이 없어도 밴드명으로 검색되도록 비정규화
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "korean", searchAnalyzer = "korean_search"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword),
                    @InnerField(suffix = "prefix", type = FieldType.Text,
                            analyzer = "korean_prefix", searchAnalyzer = "korean")
            }
    )
    private String bandName;

    // 검색 대상 (tags^2) : 곡명 검색의 주 매칭 지점 — 동의어(곡명 별칭)의 최대 수혜 필드
    @Field(type = FieldType.Text, analyzer = "korean", searchAnalyzer = "korean_search")
    private List<String> tags;

    // 검색 대상 (가중치 1, 최하위)
    @Field(type = FieldType.Text, analyzer = "korean", searchAnalyzer = "korean_search")
    private String description;

    // 필터용 : 밴드의 장르·지역을 따름 (비정규화)
    @Field(type = FieldType.Keyword)
    private String genre;

    @Field(type = FieldType.Keyword)
    private String region;

    // 정렬(동점 시 최신순)·표시용
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime uploadedAt;

    // 표시용 미리보기 이미지 (검색 대상 아님) : VIDEO=썸네일, PHOTO=첫 사진, TEXT=없음
    @Field(type = FieldType.Keyword, index = false)
    private String thumbnailUrl;

    // 표시용 밴드 프로필 이미지 (검색 대상 아님) : 밴드 변경 시 연쇄 재색인으로 최신화되는 비정규화 필드
    @Field(type = FieldType.Keyword, index = false)
    private String bandProfileImageUrl;

    // 밴드 정보 변경 시 연쇄 재색인 대상 조회용
    @Field(type = FieldType.Long)
    private Long bandId;

    // band는 반드시 fetch join으로 함께 조회된 상태여야 한다 (LAZY 프록시 추가 쿼리 방지)
    public static PostDocument from(Post post) {
        Band band = post.getBand();
        return PostDocument.builder()
                .id(post.getId())
                .docId(post.getId())
                .postType(post.getType().name())
                .title(post.getTitle())
                .bandName(band.getName())
                .tags(post.getTagList().stream().map(PostTag::getTagName).toList())
                .description(post.getDescription())
                .genre(band.getGenre().name())
                .region(band.getRegion().name())
                .uploadedAt(post.getCreatedAt())
                .thumbnailUrl(resolveThumbnailUrl(post))
                .bandProfileImageUrl(band.getProfileImageUrl())
                .bandId(band.getId())
                .build();
    }

    // 미리보기 이미지 규칙은 팬홈 카드와 동일.
    // PHOTO의 mediaList는 fetch join 불가(tagList와 List 2개 동시 fetch join 제약)라 LAZY 로딩 쿼리 1개를 유발
    // — 색인은 배치·비동기 경로에서만 실행되므로 수용
    private static String resolveThumbnailUrl(Post post) {
        return switch (post.getType()) {
            case VIDEO -> post.getThumbnailUrl();
            case PHOTO -> post.getMediaList().isEmpty() ? null
                    : post.getMediaList().getFirst().getMediaUrl();
            case TEXT -> null;
        };
    }
}

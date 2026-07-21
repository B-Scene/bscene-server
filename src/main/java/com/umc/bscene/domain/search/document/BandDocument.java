package com.umc.bscene.domain.search.document;

import com.umc.bscene.domain.band.entity.Band;
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

/**
 * 밴드 검색용 문서. 문서 ID = Band PK (재색인 시 같은 ID로 덮어써 멱등)
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// createIndex=false : 인덱스 생성은 SearchIndexService.recreateIndex()가 전담 —
// 기동 시 ES 접속을 시도하지 않아 ES가 없어도(CI·장애 상황) 앱이 정상 기동한다
@Document(indexName = "bands", createIndex = false)
@Setting(settingPath = "elasticsearch/korean-settings.json")
public class BandDocument {

    @Id
    private Long id;

    // 페이징 안정성용 tie-breaker (정렬 전용 — _id는 정렬 불가라 doc_values 필드로 복제)
    @Field(type = FieldType.Long, index = false)
    private Long docId;

    // 검색 대상 (name^3) + 완전 일치 가점용 raw + 접두어(부분 입력) 검색용 prefix
    // prefix는 색인만 잘게 썰고(korean_prefix) 검색어는 썰지 않는다(searchAnalyzer=korean) — 비대칭 필수
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "korean", searchAnalyzer = "korean_search"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword),
                    @InnerField(suffix = "prefix", type = FieldType.Text,
                            analyzer = "korean_prefix", searchAnalyzer = "korean")
            }
    )
    private String name;

    // 검색 대상 (소개글, 가중치 최하위)
    @Field(type = FieldType.Text, analyzer = "korean", searchAnalyzer = "korean_search")
    private String description;

    // 필터용
    @Field(type = FieldType.Keyword)
    private String genre;

    @Field(type = FieldType.Keyword)
    private String region;

    // 정렬(동점 시 최신 등록순)용
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    // 인기순 정렬용 팔로워 수 (정렬 전용이라 역색인 제외) — 색인 시점 스냅샷, 새벽 전체 재색인이 하루 1회 갱신
    @Field(type = FieldType.Long, index = false)
    private Long popularity;

    // 표시용 (검색 대상 아님)
    @Field(type = FieldType.Keyword, index = false)
    private String profileImageUrl;

    public static BandDocument from(Band band, long followerCount) {
        return BandDocument.builder()
                .id(band.getId())
                .docId(band.getId())
                .popularity(followerCount)
                .name(band.getName())
                .description(band.getDescription())
                .genre(band.getGenre().name())
                .region(band.getRegion().name())
                .createdAt(band.getCreatedAt())
                .profileImageUrl(band.getProfileImageUrl())
                .build();
    }
}

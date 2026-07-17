package com.umc.bscene.domain.search.document;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.performance.entity.Performance;
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

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 공연 검색용 문서. 문서 ID = Performance PK (재색인 시 같은 ID로 덮어써 멱등)
 * 색인 대상은 ACTIVE 공연만 — 소프트 삭제되면 ES에서는 문서를 삭제한다.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// createIndex=false : 인덱스 생성은 SearchIndexService.recreateIndex()가 전담 (기동 시 ES 무접촉)
@Document(indexName = "performances", createIndex = false)
@Setting(settingPath = "elasticsearch/korean-settings.json")
public class PerformanceDocument {

    @Id
    private Long id;

    // 페이징 안정성용 tie-breaker (정렬 전용 — _id는 정렬 불가라 doc_values 필드로 복제)
    @Field(type = FieldType.Long, index = false)
    private Long docId;

    // 검색 대상 (title^3) + 완전 일치 가점용 raw + 접두어(부분 입력) 검색용 prefix
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

    // 검색 대상 (가중치 최하위)
    @Field(type = FieldType.Text, analyzer = "korean", searchAnalyzer = "korean_search")
    private String description;

    // 필터용 : 공연은 밴드와 별개로 자체 장르·지역을 가짐
    @Field(type = FieldType.Keyword)
    private String genre;

    @Field(type = FieldType.Keyword)
    private String region;

    // 정렬(동점 시 최신순)·표시·D-day 계산용
    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate performanceDate;

    // 검색 대상 (장소명 검색 : "롤링홀", "이태원" 등. 가중치 최하위) + 표시용
    @Field(type = FieldType.Text, analyzer = "korean", searchAnalyzer = "korean_search")
    private String venue;

    // 표시용
    @Field(type = FieldType.Date, format = DateFormat.hour_minute_second)
    private LocalTime startTime;

    @Field(type = FieldType.Keyword, index = false)
    private String posterImageUrl;

    // 밴드 정보 변경 시 연쇄 재색인 대상 조회용
    @Field(type = FieldType.Long)
    private Long bandId;

    // band는 반드시 fetch join으로 함께 조회된 상태여야 한다 (LAZY 프록시 추가 쿼리 방지)
    public static PerformanceDocument from(Performance performance) {
        Band band = performance.getBand();
        return PerformanceDocument.builder()
                .id(performance.getId())
                .docId(performance.getId())
                .title(performance.getTitle())
                .bandName(band.getName())
                .description(performance.getDescription())
                .genre(performance.getGenre().name())
                .region(performance.getRegion().name())
                .performanceDate(performance.getPerformanceDate())
                .startTime(performance.getStartTime())
                .venue(performance.getVenue())
                .posterImageUrl(performance.getPosterImageUrl())
                .bandId(band.getId())
                .build();
    }
}

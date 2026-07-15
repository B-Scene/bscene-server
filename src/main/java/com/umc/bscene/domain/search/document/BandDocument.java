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
@Document(indexName = "bands")
@Setting(settingPath = "elasticsearch/korean-settings.json")
public class BandDocument {

    @Id
    private Long id;

    // 검색 대상 (name^3) + 완전 일치 가점용 raw
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "korean"),
            otherFields = @InnerField(suffix = "raw", type = FieldType.Keyword)
    )
    private String name;

    // 검색 대상 (소개글, 가중치 최하위)
    @Field(type = FieldType.Text, analyzer = "korean")
    private String description;

    // 필터용
    @Field(type = FieldType.Keyword)
    private String genre;

    @Field(type = FieldType.Keyword)
    private String region;

    // 정렬(동점 시 최신 등록순)용
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    // 표시용 (검색 대상 아님)
    @Field(type = FieldType.Keyword, index = false)
    private String profileImageUrl;

    public static BandDocument from(Band band) {
        return BandDocument.builder()
                .id(band.getId())
                .name(band.getName())
                .description(band.getDescription())
                .genre(band.getGenre().name())
                .region(band.getRegion().name())
                .createdAt(band.getCreatedAt())
                .profileImageUrl(band.getProfileImageUrl())
                .build();
    }
}

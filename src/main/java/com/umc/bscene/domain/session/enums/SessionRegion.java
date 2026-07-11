package com.umc.bscene.domain.session.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum SessionRegion {

    SEOUL("서울"),
    GYEONGGI("경기"),
    INCHEON("인천"),
    BUSAN("부산"),
    DAEGU("대구"),
    GWANGJU("광주"),
    DAEJEON("대전"),
    ULSAN("울산"),
    SEJONG("세종"),
    CHUNGBUK("충북"),
    CHUNGNAM("충남"),
    JEONNAM("전남"),
    JEONBUK("전북"),
    GYEONGBUK("경북"),
    GYEONGNAM("경남"),
    GANGWON("강원"),
    JEJU("제주");

    private final String description;

    SessionRegion(String description) {
        this.description = description;
    }

    @JsonCreator
    public static SessionRegion fromValue(String value) {
        for (SessionRegion region : values()) {
            if (region.name().equalsIgnoreCase(value) || region.description.equals(value)) {
                return region;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 활동 지역입니다: " + value);
    }

    // JSON 응답 시 enum 이름(SEOUL)이 아닌 한글(서울)로 반환
    @JsonValue
    public String getDescription() {
        return description;
    }
}

package com.umc.bscene.domain.session.enums;

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

    public String getDescription() {
        return description;
    }
}
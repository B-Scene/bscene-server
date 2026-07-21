package com.umc.bscene.domain.stream.enums;

// 다시보기 목록 정렬 기준
public enum ReplaySort {
    LATEST,     // 최신순 (대표 세그먼트 id 내림차순)
    POPULAR     // 인기순 (조회수 내림차순, 동률이면 최신순)
}

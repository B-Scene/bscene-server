package com.umc.bscene.domain.oauth.enums;

// 소셜 로그인 제공자 (OauthAccounts.provider)
// 주의: DB 컬럼이 MySQL 네이티브 ENUM 이라 상수를 추가하면 Flyway 마이그레이션도 함께 필요하다 (APPLE 은 V56)
public enum SocialProvider {
    KAKAO,
    // 기존 계정 데이터의 매핑을 위해 보존. 로그인·신규 가입은 지원하지 않는다.
    GOOGLE,
    // 모바일(iOS) SDK 로그인 전용. 웹 OAuth2 로그인은 미지원
    APPLE
}

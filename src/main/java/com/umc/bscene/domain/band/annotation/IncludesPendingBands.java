package com.umc.bscene.domain.band.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 쿼리(또는 저장소 전체)가 검수 중(PENDING) 밴드를 의도적으로 포함함을 선언한다.
 *
 * 공개 조회 경로는 검수 통과(ACCEPTED) 밴드만 노출해야 하고, 그 규칙은
 * BandVisibilityQueryGuardTest가 강제한다: Band를 조회·조인하는 JPQL은
 * BandStatus 조건을 갖거나, 이 애노테이션으로 "왜 PENDING을 봐야 하는지"를 선언해야 한다.
 *
 * 붙이기 전에 반드시 확인할 것 — 이 쿼리의 결과가 비멤버에게 노출되는 경로가 하나라도 있으면
 * 애노테이션이 아니라 BandStatus 조건이 답이다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IncludesPendingBands {

    // PENDING 밴드를 포함해야 하는 이유 (예: 소속 멤버 관점 조회, 검수 플로우 내부, 삭제 정리용)
    String reason();
}

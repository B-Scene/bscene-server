package com.umc.bscene.global.architecture;

import com.umc.bscene.domain.band.annotation.IncludesPendingBands;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.repository.BandRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 밴드 공개 규칙("검수 통과(ACCEPTED) 밴드만 노출") 가드.
 *
 * 이 규칙은 엔티티 그래프 공유 때문에 여러 도메인의 JPQL에 분산될 수밖에 없고,
 * 새 쿼리에서 status 조건을 빼먹는 실수는 컴파일러도 일반 테스트도 잡지 못한다
 * (실제 사례: 검수 중 밴드가 라이브를 만들 수 있었던 createStream 경로).
 *
 * 규칙:
 * 1) Band를 조회·조인하는 @Query(JPQL의 "from/join Band" 또는 ".band" 연관 탐색)는
 *    BandStatus 조건을 갖거나 @IncludesPendingBands(reason=...)로 의도를 선언해야 한다.
 * 2) BandRepository의 선언 메서드는 BandStatus 파라미터를 갖거나(파생 쿼리),
 *    BandStatus를 포함한 @Query를 갖거나, @IncludesPendingBands로 선언해야 한다.
 *
 * 범위 밖(알려진 한계):
 * - JpaRepository에서 상속되는 CRUD 통로(findById, findAll 등)의 "호출 지점"은 검사하지 못한다.
 *   밴드 도메인 내부 호출은 validateVisible이 담당한다.
 * - Band가 아닌 저장소의 파생 쿼리 메서드(deleteByBand_Id 등)는 id 스코프 연산이라 제외한다.
 *   공개 목록·피드류 쿼리는 관례상 @Query(JPQL)로 작성되므로 1)이 실질 커버리지를 가진다.
 */
class BandVisibilityQueryGuardTest {

    private static final String BASE_PACKAGE = "com/umc/bscene";

    // JPQL에서 Band 엔티티를 루트/서브쿼리로 조회 ("from Band b", "join Band")
    private static final Pattern BAND_ENTITY = Pattern.compile("\\bBand\\b");
    // 연관 탐색으로 Band에 접근 ("p.band", "f.band.id", "join fetch r.band")
    // ".bandId"(비FK Long 컬럼)나 "similarBand" 등은 단어 경계·대소문자 규칙상 매치되지 않는다
    private static final Pattern BAND_TRAVERSAL = Pattern.compile("\\.band\\b");

    @Test
    void Band를_조회하는_쿼리는_BandStatus_조건을_갖거나_PENDING_포함_의도를_선언해야_한다() throws Exception {
        List<Class<?>> repositories = findRepositoryInterfaces();
        // 스캔 자체가 무너지면(패키지 구조 변경 등) 규칙이 조용히 무력화되므로 하한선을 건다
        assertThat(repositories).hasSizeGreaterThan(15);

        List<String> violations = new ArrayList<>();

        for (Class<?> repository : repositories) {
            if (repository.isAnnotationPresent(IncludesPendingBands.class)) {
                continue;
            }

            for (Method method : repository.getDeclaredMethods()) {
                if (method.isAnnotationPresent(IncludesPendingBands.class)) {
                    continue;
                }
                if (violates(repository, method)) {
                    violations.add(repository.getSimpleName() + "." + method.getName());
                }
            }
        }

        assertThat(violations)
                .withFailMessage("""
                        Band를 조회·조인하는데 검수 상태(BandStatus) 조건이 없는 쿼리가 있습니다: %s

                        해결 방법 (둘 중 하나):
                        1) 공개 경로라면 쿼리에 BandStatus.ACCEPTED 조건을 추가하세요.
                        2) 의도적으로 PENDING 밴드를 포함해야 한다면(소속 멤버 관점, 검수 플로우, 삭제 정리 등)
                           메서드나 저장소에 @IncludesPendingBands(reason = "...")를 붙여 이유를 선언하세요.
                        검수 중 밴드가 공개 경로로 새면 검수 격리가 깨지고,
                        검수 중 밴드가 활동 데이터를 만들면 검수 거절(밴드 삭제)이 영구히 불가능해집니다.
                        """, violations)
                .isEmpty();
    }

    private boolean violates(Class<?> repository, Method method) {
        boolean hasBandStatusParameter = Arrays.asList(method.getParameterTypes()).contains(BandStatus.class);
        if (hasBandStatusParameter) {
            return false;
        }

        Query query = method.getAnnotation(Query.class);
        String jpql = query == null ? null : query.value();

        // BandRepository 선언 메서드는 항상 검사 대상 (파생 쿼리 포함)
        if (repository == BandRepository.class) {
            return jpql == null || !jpql.contains("BandStatus");
        }

        // 그 외 저장소는 Band를 조회·조인하는 @Query만 검사
        if (jpql == null) {
            return false;
        }
        boolean referencesBand = BAND_ENTITY.matcher(jpql).find() || BAND_TRAVERSAL.matcher(jpql).find();
        return referencesBand && !jpql.contains("BandStatus");
    }

    private List<Class<?>> findRepositoryInterfaces() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        SimpleMetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
        Resource[] resources = resolver.getResources("classpath*:" + BASE_PACKAGE + "/**/repository/*.class");

        List<Class<?>> repositories = new ArrayList<>();
        for (Resource resource : resources) {
            ClassMetadata metadata = metadataReaderFactory.getMetadataReader(resource).getClassMetadata();
            if (!metadata.isInterface()) {
                continue;
            }
            Class<?> candidate = Class.forName(metadata.getClassName());
            if (Repository.class.isAssignableFrom(candidate)) {
                repositories.add(candidate);
            }
        }
        return repositories;
    }
}

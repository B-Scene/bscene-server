package com.umc.bscene.domain.follow.repository;

import com.umc.bscene.domain.follow.entity.Follow;
import com.umc.bscene.domain.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    // 사용자와 밴드 사이에 팔로우 관계가 존재하는지 확인
    boolean existsByBand_IdAndUser_Id(Long bandId, Long userId);

    // 사용자와 밴드 사이의 팔로우 관계를 삭제
    void deleteByBand_IdAndUser_Id(Long bandId, Long userId);

    // 특정 밴드를 팔로우하는 사용자 수를 반환
    long countByBand_Id(Long bandId);

    // 특정 사용자가 팔로우하는 밴드 수를 반환
    long countByUser_Id(Long userId);

    // 특정 사용자가 팔로우하는 밴드 ID 목록을 반환
    @Query("SELECT f.band.id FROM Follow f WHERE f.user.id = :userId")
    List<Long> findBandIdsByUserId(@Param("userId") Long userId);

    // 특정 밴드를 팔로우하는 사용자 ID 목록을 반환 (라이브 푸시 알림 발송 대상 조회용, 활성 사용자만)
    @Query("SELECT f.user.id FROM Follow f " +
            "WHERE f.band.id = :bandId AND f.user.status = :userStatus")
    List<Long> findUserIdsByBandId(
            @Param("bandId") Long bandId,
            @Param("userStatus") UserStatus userStatus
    );

    // 후보 밴드 중 사용자가 팔로우 중인 밴드 id만 일괄 조회 (검색 결과의 팔로우 여부 표시용)
    @Query("SELECT f.band.id FROM Follow f " +
            "WHERE f.user.id = :userId AND f.band.id IN :bandIds")
    List<Long> findBandIdsByUserIdAndBandIdIn(
            @Param("userId") Long userId,
            @Param("bandIds") List<Long> bandIds
    );

    // SearchAdapter에서 사용 : 밴드별 팔로워 수 일괄 집계 (검색 인기순 popularity 색인용, [bandId, count] 행)
    @Query("SELECT f.band.id, COUNT(f) FROM Follow f GROUP BY f.band.id")
    List<Object[]> countGroupedByBand();

    // UserAdapter에서 사용 : 사용자가 팔로우한 밴드들의 장르별 밴드 수 집계 (마이페이지 대표 장르용, [genre, count] 행)
    @Query("SELECT f.band.genre, COUNT(f) FROM Follow f WHERE f.user.id = :userId GROUP BY f.band.genre")
    List<Object[]> countGroupedByGenre(@Param("userId") Long userId);
}

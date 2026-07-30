package com.umc.bscene.domain.user.repository;

import com.umc.bscene.domain.user.entity.FanProfile;
import com.umc.bscene.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FanProfileRepository extends JpaRepository<FanProfile, Long> {

    @Query("""
select fp
from FanProfile fp
    join fetch fp.user u
where u.id = :userId
""")
    Optional<FanProfile> findByUser_Id(
            @Param("userId") Long userId);

    Optional<FanProfile> findByUser(User user);

    boolean existsByNickname(String nickname);

    // 내 정보 수정의 닉네임 중복 검사 : 본인 프로필은 제외 (collation이 대소문자를 구분하지 않아도 본인 닉네임에 안 걸리게)
    boolean existsByNicknameAndUser_IdNot(String nickname, Long userId);

    // 게시물 댓글 작성자 표시용 : 여러 사용자의 팬 프로필 일괄 조회 (N+1 방지)
    List<FanProfile> findAllByUser_IdIn(Collection<Long> userIds);
}
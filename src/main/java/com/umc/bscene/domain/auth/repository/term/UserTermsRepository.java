package com.umc.bscene.domain.auth.repository.term;

import com.umc.bscene.domain.auth.entity.term.UserTerms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserTermsRepository extends JpaRepository<UserTerms, Long> {

    // 특정 약관 ID를 기준으로 약관 동의여부 조회
    @Query("""
            select distinct userTerms.user.id
            from UserTerms userTerms
            where userTerms.user.id in :userIds
              and userTerms.termId = :termId
              and userTerms.isAgreed = true
            """)
    List<Long> findAgreedUserIdsByTermId(
            @Param("userIds") Collection<Long> userIds,
            @Param("termId") Long termId
    );

}

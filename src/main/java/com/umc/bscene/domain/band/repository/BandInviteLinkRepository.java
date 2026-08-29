package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandInviteLink;
import com.umc.bscene.domain.band.enums.BandMemberType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BandInviteLinkRepository
        extends JpaRepository<BandInviteLink, Long> {

    Optional<BandInviteLink> findByBand_IdAndMemberType(
            Long bandId,
            BandMemberType memberType
    );

    Optional<BandInviteLink> findByToken(String token);

    // 검수 거절/더미 교체로 밴드를 삭제할 때 초대 링크 정리 (FK가 남으면 밴드 삭제가 실패한다)
    void deleteByBand_Id(Long bandId);
}
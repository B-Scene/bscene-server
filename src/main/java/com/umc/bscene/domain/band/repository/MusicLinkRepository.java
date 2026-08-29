package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.MusicLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MusicLinkRepository extends JpaRepository<MusicLink, Long> {

    Optional<MusicLink> findByBand_Id(Long bandId);

    // 검수 거절/더미 교체로 밴드를 삭제할 때 음악 링크 정리
    void deleteByBand_Id(Long bandId);
}

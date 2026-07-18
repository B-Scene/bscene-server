package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BandRepository extends JpaRepository<Band, Long> {

    boolean existsByName(String name);

    // 밴드 추천 후보군 조회 : 선호 장르가 일치하는 밴드
    List<Band> findByGenreIn(Collection<Genre> genres);

    // 밴드 추천 후보군 조회 : 선호 지역이 일치하는 밴드
    List<Band> findByRegionIn(Collection<Region> regions);
}

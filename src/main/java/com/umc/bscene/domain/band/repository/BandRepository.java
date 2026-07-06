package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.Band;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BandRepository extends JpaRepository<Band, Long> {

    boolean existsByName(String name);
}

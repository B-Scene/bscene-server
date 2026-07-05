package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.Band;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BandRepository extends JpaRepository<Band, Long> {

    List<Band> findByOwner_Id(Long ownerId);

    boolean existsByName(String name);
}

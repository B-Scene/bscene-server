package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.AudioStream;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioStreamRepository extends JpaRepository<AudioStream, Long> {
}

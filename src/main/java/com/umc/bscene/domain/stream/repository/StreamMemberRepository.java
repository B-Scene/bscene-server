package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamMemberRepository extends JpaRepository<StreamMember, Long> {
}

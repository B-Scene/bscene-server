package com.umc.bscene.domain.term.repository;

import com.umc.bscene.domain.term.entity.UserTerms;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermsRepository extends JpaRepository<UserTerms, Long> {
}
package com.umc.bscene.domain.auth.term.repository;

import com.umc.bscene.domain.auth.term.entity.UserTerms;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermsRepository extends JpaRepository<UserTerms, Long> {
}
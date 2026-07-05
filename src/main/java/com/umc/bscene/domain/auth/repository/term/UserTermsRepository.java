package com.umc.bscene.domain.auth.repository.term;

import com.umc.bscene.domain.auth.entity.term.UserTerms;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermsRepository extends JpaRepository<UserTerms, Long> {
}
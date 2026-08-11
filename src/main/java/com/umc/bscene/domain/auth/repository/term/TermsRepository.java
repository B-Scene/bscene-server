package com.umc.bscene.domain.auth.repository.term;

import com.umc.bscene.domain.auth.entity.term.Terms;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsRepository extends JpaRepository<Terms, Long> {
}

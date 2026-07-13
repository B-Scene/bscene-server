package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.ReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {
}

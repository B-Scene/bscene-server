package com.umc.bscene.domain.session.service;

public interface SessionApplicationService {

    Long updateStatus(
            Long applicationId,
            Boolean isApproved
    );
}
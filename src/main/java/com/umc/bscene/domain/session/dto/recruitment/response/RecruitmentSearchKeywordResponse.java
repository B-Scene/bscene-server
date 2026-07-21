package com.umc.bscene.domain.session.dto.recruitment.response;

import com.umc.bscene.domain.session.entity.SessionRecruitmentSearchKeyword;

public record RecruitmentSearchKeywordResponse(Long keywordId, String keyword) {

    public static RecruitmentSearchKeywordResponse from(SessionRecruitmentSearchKeyword searchKeyword) {
        return new RecruitmentSearchKeywordResponse(
                searchKeyword.getSessionRecruitmentSearchKeywordId(),
                searchKeyword.getKeyword()
        );
    }
}

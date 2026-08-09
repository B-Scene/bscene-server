package com.umc.bscene.domain.post.adapter;

import com.umc.bscene.domain.band.port.PostCommentPort;
import com.umc.bscene.domain.post.repository.PostCommentRepository;
import lombok.RequiredArgsConstructor;

/**
 * 밴드 도메인의 PostCommentPort를 post 도메인이 구현하는 어댑터.
 * 밴드 명의(멤버 프로필) 소멸 시 그 명의로 남은 밴드 댓글을 삭제한다.
 */
@RequiredArgsConstructor
public class BandAdapter implements PostCommentPort {

    private final PostCommentRepository postCommentRepository;

    @Override
    public void deleteBandComments(Long bandId, Long userId) {
        postCommentRepository.deleteBandComments(bandId, userId);
    }

    @Override
    public void deleteCommentsByProfileId(Long profileId) {
        postCommentRepository.deleteByBandMemberProfileId(profileId);
    }
}

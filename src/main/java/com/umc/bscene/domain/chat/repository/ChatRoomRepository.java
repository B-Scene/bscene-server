package com.umc.bscene.domain.chat.repository;

import com.umc.bscene.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findBySender_IdAndRecipient_IdAndSessionRecruitment_SessionRecruitmentId(
            Long senderId, Long recipientId, Long recruitmentId);
    Optional<ChatRoom> findBySender_IdAndRecipient_IdAndSessionApplication_SessionApplicationId(
            Long senderId, Long recipientId, Long applicationId);
}

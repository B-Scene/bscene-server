package com.umc.bscene.domain.chat.repository;

import com.umc.bscene.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findBySender_IdAndRecipient_IdAndSessionRecruitment_SessionRecruitmentId(
            Long senderId, Long recipientId, Long recruitmentId);
    Optional<ChatRoom> findBySender_IdAndRecipient_IdAndSessionApplication_SessionApplicationId(
            Long senderId, Long recipientId, Long applicationId);

    @Query("""
        SELECT room FROM ChatRoom room
        JOIN FETCH room.sender
        JOIN FETCH room.recipient
        LEFT JOIN FETCH room.sessionRecruitment recruitment
        LEFT JOIN FETCH recruitment.band
        LEFT JOIN FETCH room.sessionApplication
        WHERE (room.sender.id = :userId OR room.recipient.id = :userId)
          AND (:cursorId IS NULL OR room.chatRoomId < :cursorId)
          AND (:unreadOnly = false OR EXISTS (
              SELECT message.chatMessageId FROM ChatMessage message
              WHERE message.chatRoom = room
                AND message.sender.id <> :userId
                AND message.readAt IS NULL
          ))
        ORDER BY room.chatRoomId DESC
    """)
    List<ChatRoom> findMyRooms(@Param("userId") Long userId,
                               @Param("cursorId") Long cursorId,
                               @Param("unreadOnly") boolean unreadOnly,
                               Pageable pageable);
}

package com.umc.bscene.domain.chat.repository;

import com.umc.bscene.domain.band.annotation.IncludesPendingBands;
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
          AND ((room.sender.id = :userId AND room.senderLeftAt IS NULL)
            OR (room.recipient.id = :userId AND room.recipientLeftAt IS NULL))
          AND (:cursorId IS NULL OR
            COALESCE((
              SELECT MAX(cursorMessage.chatMessageId) FROM ChatMessage cursorMessage
              WHERE cursorMessage.chatRoom.chatRoomId = room.chatRoomId
            ), 0) < COALESCE((
              SELECT MAX(boundaryMessage.chatMessageId) FROM ChatMessage boundaryMessage
              WHERE boundaryMessage.chatRoom.chatRoomId = :cursorId
            ), 0)
            OR (
              COALESCE((
                SELECT MAX(cursorMessage.chatMessageId) FROM ChatMessage cursorMessage
                WHERE cursorMessage.chatRoom.chatRoomId = room.chatRoomId
              ), 0) = COALESCE((
                SELECT MAX(boundaryMessage.chatMessageId) FROM ChatMessage boundaryMessage
                WHERE boundaryMessage.chatRoom.chatRoomId = :cursorId
              ), 0)
              AND room.chatRoomId < :cursorId
            )
          )
          AND (:unreadOnly = false OR EXISTS (
              SELECT message.chatMessageId FROM ChatMessage message
              WHERE message.chatRoom = room
                AND message.sender.id <> :userId
                AND message.readAt IS NULL
          ))
        ORDER BY COALESCE((
          SELECT MAX(latestMessage.chatMessageId) FROM ChatMessage latestMessage
          WHERE latestMessage.chatRoom.chatRoomId = room.chatRoomId
        ), 0) DESC, room.chatRoomId DESC
    """)
    @IncludesPendingBands(reason = "지원서·채팅은 ACCEPTED 밴드의 모집에서만 파생된다")
    List<ChatRoom> findMyRooms(@Param("userId") Long userId,
                               @Param("cursorId") Long cursorId,
                               @Param("unreadOnly") boolean unreadOnly,
                               Pageable pageable);

    @Query("""
        SELECT room FROM ChatRoom room
        JOIN FETCH room.sender
        JOIN FETCH room.recipient
        LEFT JOIN FETCH room.sessionRecruitment recruitment
        LEFT JOIN FETCH recruitment.band
        LEFT JOIN FETCH room.sessionApplication
        WHERE room.chatRoomId = :roomId
    """)
    @IncludesPendingBands(reason = "지원서·채팅은 ACCEPTED 밴드의 모집에서만 파생된다")
    Optional<ChatRoom> findDetail(@Param("roomId") Long roomId);
}

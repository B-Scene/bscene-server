package com.umc.bscene.domain.chat.repository;

import com.umc.bscene.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("""
        SELECT message FROM ChatMessage message
        JOIN FETCH message.sender
        WHERE message.chatRoom.chatRoomId IN :roomIds
        ORDER BY message.chatMessageId DESC
    """)
    List<ChatMessage> findMessagesForRooms(@Param("roomIds") Collection<Long> roomIds);

    @Query("""
        SELECT message FROM ChatMessage message
        JOIN FETCH message.sender
        WHERE message.chatRoom.chatRoomId = :roomId
          AND (:cursorId IS NULL OR message.chatMessageId < :cursorId)
        ORDER BY message.chatMessageId DESC
    """)
    List<ChatMessage> findRoomMessages(
            @Param("roomId") Long roomId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Modifying
    @Query("""
        UPDATE ChatMessage message
        SET message.readAt = :readAt
        WHERE message.chatRoom.chatRoomId = :roomId
          AND message.sender.id <> :readerId
          AND message.readAt IS NULL
    """)
    int markAllUnreadAsRead(
            @Param("roomId") Long roomId,
            @Param("readerId") Long readerId,
            @Param("readAt") LocalDateTime readAt
    );

    boolean existsByChatMessageIdAndChatRoom_ChatRoomId(
            Long chatMessageId,
            Long chatRoomId
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ChatMessage message
        SET message.readAt = :readAt
        WHERE message.chatRoom.chatRoomId = :roomId
          AND message.sender.id <> :readerId
          AND message.chatMessageId <= :lastReadMessageId
          AND message.readAt IS NULL
    """)
    int markReadThrough(
            @Param("roomId") Long roomId,
            @Param("readerId") Long readerId,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("readAt") LocalDateTime readAt
    );
}

package com.umc.bscene.domain.chat.repository;

import com.umc.bscene.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("""
        SELECT message FROM ChatMessage message
        JOIN FETCH message.sender
        WHERE message.chatRoom.chatRoomId IN :roomIds
        ORDER BY message.chatMessageId DESC
    """)
    List<ChatMessage> findMessagesForRooms(@Param("roomIds") Collection<Long> roomIds);
}

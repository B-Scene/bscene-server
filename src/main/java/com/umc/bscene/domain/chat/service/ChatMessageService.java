package com.umc.bscene.domain.chat.service;

import com.umc.bscene.domain.chat.dto.request.ChatMessageSendRequest;
import com.umc.bscene.domain.chat.dto.request.ChatMessageReadRequest;
import com.umc.bscene.domain.chat.dto.response.ChatMessageReadPushData;
import com.umc.bscene.domain.chat.dto.response.ChatMessageReadResult;
import com.umc.bscene.domain.chat.dto.response.ChatMessagePushData;
import com.umc.bscene.domain.chat.dto.response.ChatMessageSendResult;
import com.umc.bscene.domain.chat.entity.ChatMessage;
import com.umc.bscene.domain.chat.entity.ChatRoom;
import com.umc.bscene.domain.chat.enums.ChatContextType;
import com.umc.bscene.domain.chat.exception.ChatException;
import com.umc.bscene.domain.chat.repository.ChatMessageRepository;
import com.umc.bscene.domain.chat.repository.ChatRoomRepository;
import com.umc.bscene.domain.chat.response.code.ChatErrorCode;
import com.umc.bscene.domain.chat.response.code.ChatWebSocketErrorCode;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionBasicProfileRepository;
import com.umc.bscene.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private static final int MAX_CONTENT_LENGTH = 2_000;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SessionApplicationSubmissionRepository submissionRepository;
    private final SessionBasicProfileRepository sessionBasicProfileRepository;

    @Transactional
    public ChatMessageSendResult send(Long userId, ChatMessageSendRequest request) {
        if (request == null || request.chatRoomId() == null) {
            throw new ChatException(ChatWebSocketErrorCode.INVALID_FRAME);
        }

        String content = normalizeContent(request.content());
        ChatRoom room = chatRoomRepository.findDetail(request.chatRoomId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        User sender = resolveParticipant(room, userId);

        if (room.hasLeft(userId)) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        if (!canSend(room)) {
            throw new ChatException(ChatWebSocketErrorCode.SEND_NOT_ALLOWED);
        }

        Long recipientId = room.getSender().getId().equals(userId)
                ? room.getRecipient().getId()
                : room.getSender().getId();
        room.rejoin(recipientId);

        ChatMessage message = chatMessageRepository.saveAndFlush(ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .build());

        String profileImageUrl = sessionBasicProfileRepository.findByUser_Id(userId)
                .map(profile -> profile.getProfileImageUrl())
                .orElse(null);

        return new ChatMessageSendResult(
                recipientId,
                new ChatMessagePushData(
                        message.getChatMessageId(),
                        room.getChatRoomId(),
                        userId,
                        sender.getName(),
                        profileImageUrl,
                        message.getContent(),
                        null,
                        message.getCreatedAt().format(DATE_TIME_FORMATTER)
                )
        );
    }

    @Transactional
    public ChatMessageReadResult markRead(Long userId, ChatMessageReadRequest request) {
        if (request == null
                || request.chatRoomId() == null
                || request.lastReadMessageId() == null) {
            throw new ChatException(ChatWebSocketErrorCode.INVALID_FRAME);
        }

        ChatRoom room = chatRoomRepository.findDetail(request.chatRoomId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        resolveParticipant(room, userId);
        if (room.hasLeft(userId)) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        if (!chatMessageRepository.existsByChatMessageIdAndChatRoom_ChatRoomId(
                request.lastReadMessageId(), request.chatRoomId())) {
            throw new ChatException(ChatWebSocketErrorCode.READ_MESSAGE_NOT_FOUND);
        }

        LocalDateTime readAt = LocalDateTime.now();
        int updatedMessages = chatMessageRepository.markReadThrough(
                request.chatRoomId(), userId, request.lastReadMessageId(), readAt);

        Long counterpartId = room.getSender().getId().equals(userId)
                ? room.getRecipient().getId()
                : room.getSender().getId();
        return new ChatMessageReadResult(
                counterpartId,
                new ChatMessageReadPushData(
                        request.chatRoomId(),
                        userId,
                        request.lastReadMessageId(),
                        readAt.format(DATE_TIME_FORMATTER)
                ),
                updatedMessages > 0
        );
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new ChatException(ChatWebSocketErrorCode.EMPTY_CONTENT);
        }

        String normalized = content.strip();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new ChatException(ChatWebSocketErrorCode.CONTENT_TOO_LONG);
        }
        return normalized;
    }

    private User resolveParticipant(ChatRoom room, Long userId) {
        if (room.getSender().getId().equals(userId)) return room.getSender();
        if (room.getRecipient().getId().equals(userId)) return room.getRecipient();
        throw new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    private boolean canSend(ChatRoom room) {
        if (room.getContextType() != ChatContextType.RECRUITMENT) return true;

        return submissionRepository
                .findFirstBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_UserIdOrderByApplicationSubmissionIdDesc(
                        room.getSessionRecruitment().getSessionRecruitmentId(),
                        room.getSender().getId())
                .map(submission -> submission.getStatus() != ApplicationStatus.REJECTED)
                .orElse(true);
    }
}

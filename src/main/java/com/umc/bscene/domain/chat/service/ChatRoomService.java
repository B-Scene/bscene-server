package com.umc.bscene.domain.chat.service;

import com.umc.bscene.domain.chat.dto.request.ChatRoomCreateRequest;
import com.umc.bscene.domain.chat.dto.response.ChatRoomCreateResponse;
import com.umc.bscene.domain.chat.dto.response.ChatRoomListItemResponse;
import com.umc.bscene.domain.chat.dto.response.ChatRoomListResponse;
import com.umc.bscene.domain.chat.dto.response.ChatRoomDetailResponse;
import com.umc.bscene.domain.chat.dto.response.ChatMessageDetailResponse;
import com.umc.bscene.domain.chat.entity.ChatMessage;
import com.umc.bscene.domain.chat.enums.ChatRoomFilter;
import com.umc.bscene.domain.chat.repository.ChatMessageRepository;
import com.umc.bscene.domain.chat.entity.ChatRoom;
import com.umc.bscene.domain.chat.enums.ChatContextType;
import com.umc.bscene.domain.chat.exception.ChatException;
import com.umc.bscene.domain.chat.repository.ChatRoomRepository;
import com.umc.bscene.domain.chat.enums.code.error.ChatErrorCode;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionBasicProfileRepository;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {
    private static final String DEFAULT_PURPOSE = "기본";

    private final ChatRoomRepository chatRoomRepository;
    private final SessionRecruitmentRepository recruitmentRepository;
    private final SessionApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SessionApplicationSubmissionRepository submissionRepository;
    private final SessionBasicProfileRepository sessionBasicProfileRepository;

    @Transactional(readOnly = true)
    public ChatRoomListResponse getMyRooms(Long userId, ChatRoomFilter filter,
                                           Long cursorId, Integer size) {
        int pageSize = size == null ? 20 : Math.max(1, Math.min(size, 50));
        boolean unreadOnly = filter == ChatRoomFilter.UNREAD;
        List<ChatRoom> rooms = chatRoomRepository.findMyRooms(
                userId, cursorId, unreadOnly, PageRequest.of(0, pageSize + 1));
        boolean hasNext = rooms.size() > pageSize;
        List<ChatRoom> sliced = hasNext ? rooms.subList(0, pageSize) : rooms;
        List<Long> roomIds = sliced.stream().map(ChatRoom::getChatRoomId).toList();
        List<ChatMessage> messages = roomIds.isEmpty()
                ? List.of() : chatMessageRepository.findMessagesForRooms(roomIds);
        Map<Long, ChatMessage> latestMessages = messages.stream().collect(Collectors.toMap(
                message -> message.getChatRoom().getChatRoomId(), Function.identity(),
                (latest, ignored) -> latest));
        Map<Long, Long> unreadCounts = messages.stream()
                .filter(message -> !message.getSender().getId().equals(userId))
                .filter(message -> message.getReadAt() == null)
                .collect(Collectors.groupingBy(
                        message -> message.getChatRoom().getChatRoomId(), Collectors.counting()));
        List<ChatRoomListItemResponse> content = sliced.stream()
                .map(room -> {
                    ApplicationStatus applicationStatus = resolveApplicationStatus(room);
                    SessionApplication counterpartApplication =
                            room.getContextType() == ChatContextType.SESSION_SEARCH
                                    ? resolveSessionSearchCounterpartApplication(room, userId)
                                    : null;
                    return ChatRoomListItemResponse.of(
                            room, userId, latestMessages.get(room.getChatRoomId()),
                            unreadCounts.getOrDefault(room.getChatRoomId(), 0L),
                            applicationStatus != ApplicationStatus.REJECTED,
                            applicationStatusLabel(applicationStatus),
                            counterpartApplication,
                            resolveCounterpartProfileImageUrl(room, userId));
                })
                .toList();
        Long nextCursor = hasNext && !sliced.isEmpty()
                ? sliced.get(sliced.size() - 1).getChatRoomId() : null;
        return new ChatRoomListResponse(content, pageSize, nextCursor, hasNext);
    }

    public ChatRoomDetailResponse getRoomDetail(
            Long userId, Long roomId, Long cursorId, Integer size) {
        ChatRoom room = chatRoomRepository.findDetail(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        boolean viewerIsSender = room.getSender().getId().equals(userId);
        boolean viewerIsRecipient = room.getRecipient().getId().equals(userId);
        if (!viewerIsSender && !viewerIsRecipient) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        if (room.hasLeft(userId)) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        int pageSize = size == null ? 20 : Math.max(1, Math.min(size, 50));
        chatMessageRepository.markAllUnreadAsRead(roomId, userId, LocalDateTime.now());
        List<ChatMessage> fetchedMessages = chatMessageRepository.findRoomMessages(
                roomId, cursorId, PageRequest.of(0, pageSize + 1));
        boolean hasNext = fetchedMessages.size() > pageSize;
        List<ChatMessage> messages = new ArrayList<>(hasNext
                ? fetchedMessages.subList(0, pageSize)
                : fetchedMessages);
        Collections.reverse(messages);
        Long nextCursor = hasNext && !messages.isEmpty()
                ? messages.get(0).getChatMessageId()
                : null;

        SessionApplication counterpartApplication =
                room.getContextType() == ChatContextType.RECRUITMENT
                        ? findRecruitmentCounterpartApplication(room)
                        : resolveSessionSearchCounterpartApplication(room, userId);
        String profileImageUrl = room.getContextType() == ChatContextType.RECRUITMENT
                && viewerIsSender
                ? room.getSessionRecruitment().getBand().getProfileImageUrl()
                : resolveCounterpartProfileImageUrl(room, userId);
        Long opponentId = viewerIsSender
                ? room.getRecipient().getId() : room.getSender().getId();
        String opponentName;
        Long recruitmentId = null;
        Long applicationId = null;
        Long applicationSubmissionId = null;

        if (room.getContextType() == ChatContextType.RECRUITMENT) {
            recruitmentId = room.getSessionRecruitment().getSessionRecruitmentId();
            applicationId = counterpartApplication == null
                    ? null : counterpartApplication.getSessionApplicationId();
            if (viewerIsRecipient) {
                applicationSubmissionId = submissionRepository
                        .findFirstBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_UserIdOrderByApplicationSubmissionIdDesc(
                                recruitmentId, room.getSender().getId())
                        .map(submission -> submission.getApplicationSubmissionId())
                        .orElse(null);
            }
            opponentName = viewerIsSender
                    ? room.getSessionRecruitment().getBand().getName()
                    : room.getSender().getName();
        } else {
            applicationId = counterpartApplication.getSessionApplicationId();
            opponentName = viewerIsSender
                    ? room.getRecipient().getName() : room.getSender().getName();
        }

        return new ChatRoomDetailResponse(
                roomId, room.getContextType(), applicationId, recruitmentId, applicationSubmissionId,
                opponentId, opponentName, profileImageUrl, canSend(room),
                messages.stream()
                        .map(message -> ChatMessageDetailResponse.from(message, userId))
                        .toList(),
                pageSize, nextCursor, hasNext
        );
    }

    private boolean canSend(ChatRoom room) {
        return resolveApplicationStatus(room) != ApplicationStatus.REJECTED;
    }

    private ApplicationStatus resolveApplicationStatus(ChatRoom room) {
        if (room.getContextType() != ChatContextType.RECRUITMENT) return null;
        return submissionRepository
                .findFirstBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_UserIdOrderByApplicationSubmissionIdDesc(
                        room.getSessionRecruitment().getSessionRecruitmentId(), room.getSender().getId())
                .map(submission -> submission.getStatus())
                .orElse(null);
    }

    private String applicationStatusLabel(ApplicationStatus status) {
        if (status == ApplicationStatus.ACCEPTED) return "수락";
        if (status == ApplicationStatus.REJECTED) return "거절";
        return null;
    }

    private SessionApplication findRecruitmentCounterpartApplication(ChatRoom room) {
        return submissionRepository
                .findFirstBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_UserIdOrderByApplicationSubmissionIdDesc(
                        room.getSessionRecruitment().getSessionRecruitmentId(),
                        room.getSender().getId())
                .map(submission -> submission.getSessionApplication())
                .orElseGet(() -> applicationRepository
                        .findFirstByUserIdAndPurposeAndDeletedAtIsNullOrderBySessionApplicationIdDesc(
                                room.getSender().getId(), DEFAULT_PURPOSE)
                        .orElse(null));
    }

    private SessionApplication resolveSessionSearchCounterpartApplication(
            ChatRoom room, Long viewerId) {
        if (room.getSender().getId().equals(viewerId)) {
            return room.getSessionApplication();
        }
        return applicationRepository
                .findFirstByUserIdAndPurposeAndDeletedAtIsNullOrderBySessionApplicationIdDesc(
                        room.getSender().getId(), DEFAULT_PURPOSE)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_TARGET_NOT_FOUND));
    }

    private String resolveCounterpartProfileImageUrl(ChatRoom room, Long viewerId) {
        if (room.getContextType() == ChatContextType.RECRUITMENT
                && room.getSender().getId().equals(viewerId)) return null;
        Long counterpartUserId = room.getSender().getId().equals(viewerId)
                ? room.getRecipient().getId()
                : room.getSender().getId();
        return sessionBasicProfileRepository.findByUser_Id(counterpartUserId)
                .map(profile -> profile.getProfileImageUrl())
                .orElse(null);
    }

    public ChatRoomCreateResponse createOrGet(Long senderId, ChatRoomCreateRequest request) {
        if (request.contextType() == ChatContextType.RECRUITMENT) {
            return createForRecruitment(senderId, request);
        }
        if (request.contextType() == ChatContextType.SESSION_SEARCH) {
            return createForSessionSearch(senderId, request);
        }
        throw new ChatException(ChatErrorCode.INVALID_CHAT_CONTEXT);
    }

    public void leaveRoom(Long userId, Long roomId) {
        ChatRoom room = chatRoomRepository.findDetail(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        boolean participant = room.getSender().getId().equals(userId)
                || room.getRecipient().getId().equals(userId);
        if (!participant) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        if (room.hasLeft(userId)) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        room.leave(userId);
    }

    private ChatRoomCreateResponse createForRecruitment(Long senderId, ChatRoomCreateRequest request) {
        if (request.applicationSubmissionId() != null) {
            return createForSubmittedApplication(senderId, request);
        }
        if (request.sessionRecruitmentId() == null || request.sessionApplicationId() != null) {
            throw new ChatException(ChatErrorCode.INVALID_CHAT_CONTEXT);
        }
        SessionRecruitment recruitment = recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(request.sessionRecruitmentId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_TARGET_NOT_FOUND));
        User recipient = recruitment.getBand().getOwner();
        validateNotSelf(senderId, recipient.getId());
        validateRecruitmentChatAllowed(recruitment.getSessionRecruitmentId(), senderId);

        return chatRoomRepository
                .findBySender_IdAndRecipient_IdAndSessionRecruitment_SessionRecruitmentId(
                        senderId, recipient.getId(), recruitment.getSessionRecruitmentId())
                .map(room -> {
                    room.rejoin(senderId);
                    return ChatRoomCreateResponse.recruitment(room, senderId, false);
                })
                .orElseGet(() -> ChatRoomCreateResponse.recruitment(
                        chatRoomRepository.save(ChatRoom.builder()
                                .contextType(ChatContextType.RECRUITMENT)
                                .sender(userRepository.getReferenceById(senderId))
                                .recipient(recipient)
                                .sessionRecruitment(recruitment)
                                .build()), senderId, true));
    }

    private ChatRoomCreateResponse createForSubmittedApplication(
            Long viewerId, ChatRoomCreateRequest request) {
        if (request.sessionRecruitmentId() != null || request.sessionApplicationId() != null) {
            throw new ChatException(ChatErrorCode.INVALID_CHAT_CONTEXT);
        }
        SessionApplicationSubmission submission = submissionRepository
                .findForRecruitmentMember(request.applicationSubmissionId(), viewerId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_TARGET_NOT_FOUND));
        Long bandOwnerId = submission.getSessionRecruitment()
                .getBand()
                .getOwner()
                .getId();
        if (!bandOwnerId.equals(viewerId)) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        if (submission.getStatus() == ApplicationStatus.REJECTED) {
            throw new ChatException(ChatErrorCode.REJECTED_APPLICATION_CHAT_NOT_ALLOWED);
        }

        SessionRecruitment recruitment = recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(
                        submission.getSessionRecruitment().getSessionRecruitmentId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_TARGET_NOT_FOUND));
        Long applicantId = submission.getSessionApplication().getUserId();
        validateNotSelf(viewerId, applicantId);
        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_TARGET_NOT_FOUND));

        return chatRoomRepository
                .findBySender_IdAndRecipient_IdAndSessionRecruitment_SessionRecruitmentId(
                        applicantId, viewerId, recruitment.getSessionRecruitmentId())
                .map(room -> {
                    room.rejoin(viewerId);
                    return ChatRoomCreateResponse.recruitment(room, viewerId, false);
                })
                .orElseGet(() -> ChatRoomCreateResponse.recruitment(
                        chatRoomRepository.save(ChatRoom.builder()
                                .contextType(ChatContextType.RECRUITMENT)
                                .sender(applicant)
                                .recipient(userRepository.getReferenceById(viewerId))
                                .sessionRecruitment(recruitment)
                                .build()), viewerId, true));
    }

    private ChatRoomCreateResponse createForSessionSearch(Long senderId, ChatRoomCreateRequest request) {
        if (request.sessionApplicationId() == null
                || request.sessionRecruitmentId() != null
                || request.applicationSubmissionId() != null) {
            throw new ChatException(ChatErrorCode.INVALID_CHAT_CONTEXT);
        }
        SessionApplication myDefault = applicationRepository
                .findFirstByUserIdAndPurposeAndDeletedAtIsNullOrderBySessionApplicationIdDesc(
                        senderId, DEFAULT_PURPOSE)
                .filter(application -> Boolean.TRUE.equals(application.getIsPublic()))
                .orElseThrow(() -> new ChatException(
                        ChatErrorCode.PUBLIC_SESSION_PROFILE_REQUIRED));
        SessionApplication target = applicationRepository
                .findPublicDetailWithPortfolioLinks(request.sessionApplicationId(), DEFAULT_PURPOSE)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_TARGET_NOT_FOUND));
        validateNotSelf(senderId, target.getUserId());
        User recipient = userRepository.findById(target.getUserId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_TARGET_NOT_FOUND));

        return chatRoomRepository
                .findBySender_IdAndRecipient_IdAndSessionApplication_SessionApplicationId(
                        senderId, recipient.getId(), target.getSessionApplicationId())
                .map(room -> {
                    room.rejoin(senderId);
                    return ChatRoomCreateResponse.sessionSearch(room, false);
                })
                .orElseGet(() -> ChatRoomCreateResponse.sessionSearch(
                        chatRoomRepository.save(ChatRoom.builder()
                                .contextType(ChatContextType.SESSION_SEARCH)
                                .sender(userRepository.getReferenceById(senderId))
                                .recipient(recipient)
                                .sessionApplication(target)
                                .build()), true));
    }

    private void validateNotSelf(Long senderId, Long recipientId) {
        if (senderId.equals(recipientId)) {
            throw new ChatException(ChatErrorCode.SELF_CHAT_NOT_ALLOWED);
        }
    }

    private void validateRecruitmentChatAllowed(Long recruitmentId, Long senderId) {
        submissionRepository
                .findFirstBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_UserIdOrderByApplicationSubmissionIdDesc(
                        recruitmentId,
                        senderId
                )
                .filter(submission -> submission.getStatus() == ApplicationStatus.REJECTED)
                .ifPresent(submission -> {
                    throw new ChatException(
                            ChatErrorCode.REJECTED_APPLICATION_CHAT_NOT_ALLOWED
                    );
                });
    }
}

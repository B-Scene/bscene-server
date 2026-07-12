package com.umc.bscene.domain.chat.service;

import com.umc.bscene.domain.chat.dto.request.ChatRoomCreateRequest;
import com.umc.bscene.domain.chat.dto.response.ChatRoomCreateResponse;
import com.umc.bscene.domain.chat.entity.ChatRoom;
import com.umc.bscene.domain.chat.enums.ChatContextType;
import com.umc.bscene.domain.chat.exception.ChatException;
import com.umc.bscene.domain.chat.repository.ChatRoomRepository;
import com.umc.bscene.domain.chat.response.code.ChatErrorCode;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {
    private static final String DEFAULT_PURPOSE = "기본";

    private final ChatRoomRepository chatRoomRepository;
    private final SessionRecruitmentRepository recruitmentRepository;
    private final SessionApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public ChatRoomCreateResponse createOrGet(Long senderId, ChatRoomCreateRequest request) {
        if (request.contextType() == ChatContextType.RECRUITMENT) {
            return createForRecruitment(senderId, request);
        }
        if (request.contextType() == ChatContextType.SESSION_SEARCH) {
            return createForSessionSearch(senderId, request);
        }
        throw new ChatException(ChatErrorCode.INVALID_CHAT_CONTEXT);
    }

    private ChatRoomCreateResponse createForRecruitment(Long senderId, ChatRoomCreateRequest request) {
        if (request.sessionRecruitmentId() == null || request.sessionApplicationId() != null) {
            throw new ChatException(ChatErrorCode.INVALID_CHAT_CONTEXT);
        }
        SessionRecruitment recruitment = recruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(request.sessionRecruitmentId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_TARGET_NOT_FOUND));
        User recipient = recruitment.getBand().getOwner();
        validateNotSelf(senderId, recipient.getId());

        return chatRoomRepository
                .findBySender_IdAndRecipient_IdAndSessionRecruitment_SessionRecruitmentId(
                        senderId, recipient.getId(), recruitment.getSessionRecruitmentId())
                .map(room -> ChatRoomCreateResponse.recruitment(room, false))
                .orElseGet(() -> ChatRoomCreateResponse.recruitment(
                        chatRoomRepository.save(ChatRoom.builder()
                                .contextType(ChatContextType.RECRUITMENT)
                                .sender(userRepository.getReferenceById(senderId))
                                .recipient(recipient)
                                .sessionRecruitment(recruitment)
                                .build()), true));
    }

    private ChatRoomCreateResponse createForSessionSearch(Long senderId, ChatRoomCreateRequest request) {
        if (request.sessionApplicationId() == null || request.sessionRecruitmentId() != null) {
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
                .map(room -> ChatRoomCreateResponse.sessionSearch(room, false))
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
}

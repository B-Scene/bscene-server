package com.umc.bscene.domain.chat.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.chat.dto.request.ChatRoomCreateRequest;
import com.umc.bscene.domain.chat.entity.ChatRoom;
import com.umc.bscene.domain.chat.enums.ChatContextType;
import com.umc.bscene.domain.chat.enums.ChatRoomFilter;
import com.umc.bscene.domain.chat.enums.code.error.ChatErrorCode;
import com.umc.bscene.domain.chat.exception.ChatException;
import com.umc.bscene.domain.chat.repository.ChatMessageRepository;
import com.umc.bscene.domain.chat.repository.ChatRoomRepository;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionBasicProfileRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;
    private static final Long APPLICATION_ID = 10L;
    private static final String DEFAULT_PURPOSE = "기본";

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private SessionRecruitmentRepository recruitmentRepository;
    @Mock
    private SessionApplicationRepository applicationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private SessionApplicationSubmissionRepository submissionRepository;
    @Mock
    private SessionBasicProfileRepository basicProfileRepository;

    private ChatRoomService service;

    @BeforeEach
    void setUp() {
        service = new ChatRoomService(
                chatRoomRepository,
                recruitmentRepository,
                applicationRepository,
                userRepository,
                chatMessageRepository,
                submissionRepository,
                basicProfileRepository
        );
    }

    @Test
    @DisplayName("세션찾기 지원서를 대상으로 새 쪽지방을 생성한다")
    void createSessionSearchRoomSuccess() {
        SessionApplication myDefault =
                application(5L, USER_ID, "내 프로필");
        SessionApplication target =
                application(APPLICATION_ID, TARGET_USER_ID, "상대 프로필");
        User sender = user(USER_ID, "나");
        User recipient = user(TARGET_USER_ID, "상대");
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                ChatContextType.SESSION_SEARCH,
                null,
                APPLICATION_ID,
                null
        );
        when(applicationRepository
                .findFirstByUserIdAndPurposeAndDeletedAtIsNullOrderBySessionApplicationIdDesc(
                        USER_ID, DEFAULT_PURPOSE
                )).thenReturn(Optional.of(myDefault));
        when(applicationRepository.findPublicDetailWithPortfolioLinks(
                APPLICATION_ID, DEFAULT_PURPOSE
        )).thenReturn(Optional.of(target));
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(recipient));
        when(chatRoomRepository
                .findBySender_IdAndRecipient_IdAndSessionApplication_SessionApplicationId(
                        USER_ID, TARGET_USER_ID, APPLICATION_ID
                )).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(sender);
        when(chatRoomRepository.save(any(ChatRoom.class)))
                .thenAnswer(invocation -> {
                    ChatRoom room = invocation.getArgument(0);
                    ReflectionTestUtils.setField(room, "chatRoomId", 100L);
                    return room;
                });

        var response = service.createOrGet(USER_ID, request);

        assertThat(response.chatRoomId()).isEqualTo(100L);
        assertThat(response.contextType()).isEqualTo(ChatContextType.SESSION_SEARCH);
        assertThat(response.created()).isTrue();
        assertThat(response.recipientUserId()).isEqualTo(TARGET_USER_ID);
    }

    @Test
    @DisplayName("기존 쪽지방이 있으면 새로 저장하지 않고 나가기 상태를 복구한다")
    void createSessionSearchRoomReusesAndRejoinsExistingRoom() {
        SessionApplication myDefault =
                application(5L, USER_ID, "내 프로필");
        SessionApplication target =
                application(APPLICATION_ID, TARGET_USER_ID, "상대 프로필");
        User recipient = user(TARGET_USER_ID, "상대");
        ChatRoom existingRoom = room(100L);
        existingRoom.leave(USER_ID);
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                ChatContextType.SESSION_SEARCH,
                null,
                APPLICATION_ID,
                null
        );
        when(applicationRepository
                .findFirstByUserIdAndPurposeAndDeletedAtIsNullOrderBySessionApplicationIdDesc(
                        USER_ID, DEFAULT_PURPOSE
                )).thenReturn(Optional.of(myDefault));
        when(applicationRepository.findPublicDetailWithPortfolioLinks(
                APPLICATION_ID, DEFAULT_PURPOSE
        )).thenReturn(Optional.of(target));
        when(userRepository.findById(TARGET_USER_ID))
                .thenReturn(Optional.of(recipient));
        when(chatRoomRepository
                .findBySender_IdAndRecipient_IdAndSessionApplication_SessionApplicationId(
                        USER_ID, TARGET_USER_ID, APPLICATION_ID
                )).thenReturn(Optional.of(existingRoom));

        var response = service.createOrGet(USER_ID, request);

        assertThat(response.chatRoomId()).isEqualTo(100L);
        assertThat(response.created()).isFalse();
        assertThat(existingRoom.hasLeft(USER_ID)).isFalse();
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("지원서와 모집 공고가 함께 전달되면 잘못된 채팅 문맥으로 실패한다")
    void createRoomFailsForInvalidContext() {
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                ChatContextType.SESSION_SEARCH,
                20L,
                APPLICATION_ID,
                null
        );

        assertThatThrownBy(() -> service.createOrGet(USER_ID, request))
                .isInstanceOf(ChatException.class)
                .extracting("baseResponseCode")
                .isEqualTo(ChatErrorCode.INVALID_CHAT_CONTEXT);
    }

    @Test
    @DisplayName("내 쪽지방 목록을 커서 방식으로 조회한다")
    void getMyRoomsCalculatesNextCursor() {
        ChatRoom first = room(100L);
        ChatRoom second = room(99L);
        when(chatRoomRepository.findMyRooms(
                eq(USER_ID), eq(null), eq(false), any(Pageable.class)
        )).thenReturn(List.of(first, second));
        when(chatMessageRepository.findMessagesForRooms(List.of(100L)))
                .thenReturn(List.of());
        when(basicProfileRepository.findByUser_Id(TARGET_USER_ID))
                .thenReturn(Optional.empty());

        var response = service.getMyRooms(
                USER_ID, ChatRoomFilter.ALL, null, 1
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(100L);
        assertThat(response.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("안 읽은 쪽지 필터와 최대 페이지 크기를 Repository에 전달한다")
    void getMyRoomsUsesUnreadFilterAndClampsSize() {
        when(chatRoomRepository.findMyRooms(
                eq(USER_ID), eq(null), eq(true), any(Pageable.class)
        )).thenReturn(List.of());

        var response = service.getMyRooms(
                USER_ID, ChatRoomFilter.UNREAD, null, 100
        );

        assertThat(response.size()).isEqualTo(50);
        assertThat(response.content()).isEmpty();
    }

    @Test
    @DisplayName("모집공고 쪽지방 목록은 상대 지원서가 없어도 조회한다")
    void getMyRoomsDoesNotRequireApplicationForRecruitmentRoom() {
        ChatRoom room = recruitmentRoom(100L, 20L);
        when(chatRoomRepository.findMyRooms(
                eq(USER_ID), eq(null), eq(false), any(Pageable.class)
        )).thenReturn(List.of(room));
        when(chatMessageRepository.findMessagesForRooms(List.of(100L)))
                .thenReturn(List.of());

        var response = service.getMyRooms(
                USER_ID, ChatRoomFilter.ALL, null, 20
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).contextType())
                .isEqualTo(ChatContextType.RECRUITMENT);
        assertThat(response.content().get(0).contextId()).isEqualTo(20L);
        verifyNoInteractions(applicationRepository);
    }

    @Test
    @DisplayName("참여 중인 쪽지방 상세정보를 조회하고 읽음 처리한다")
    void getRoomDetailSuccess() {
        ChatRoom room = room(100L);
        when(chatRoomRepository.findDetail(100L)).thenReturn(Optional.of(room));
        when(chatMessageRepository.findRoomMessages(
                eq(100L), eq(null), any(Pageable.class)
        )).thenReturn(List.of());
        when(basicProfileRepository.findByUser_Id(TARGET_USER_ID))
                .thenReturn(Optional.empty());

        var response = service.getRoomDetail(USER_ID, 100L, null, null);

        verify(chatMessageRepository).markAllUnreadAsRead(
                eq(100L), eq(USER_ID), any()
        );
        assertThat(response.chatRoomId()).isEqualTo(100L);
        assertThat(response.opponentUserId()).isEqualTo(TARGET_USER_ID);
        assertThat(response.messages()).isEmpty();
        assertThat(response.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("모집공고 쪽지방 상세는 상대 지원서가 없어도 조회한다")
    void getRecruitmentRoomDetailWithoutApplication() {
        ChatRoom room = recruitmentRoom(100L, 20L);
        when(chatRoomRepository.findDetail(100L)).thenReturn(Optional.of(room));
        when(chatMessageRepository.findRoomMessages(
                eq(100L), eq(null), any(Pageable.class)
        )).thenReturn(List.of());

        var response = service.getRoomDetail(USER_ID, 100L, null, 20);

        assertThat(response.contextType()).isEqualTo(ChatContextType.RECRUITMENT);
        assertThat(response.sessionApplicationId()).isNull();
        assertThat(response.sessionRecruitmentId()).isEqualTo(20L);
        assertThat(response.opponentName()).isEqualTo("테스트 밴드");
        assertThat(response.messages()).isEmpty();
    }

    @Test
    @DisplayName("참여자가 아닌 사용자는 쪽지방 상세정보를 조회할 수 없다")
    void getRoomDetailFailsForNonParticipant() {
        ChatRoom room = room(100L);
        when(chatRoomRepository.findDetail(100L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.getRoomDetail(
                999L, 100L, null, 20
        ))
                .isInstanceOf(ChatException.class)
                .extracting("baseResponseCode")
                .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        verify(chatMessageRepository, never())
                .markAllUnreadAsRead(any(), any(), any());
    }

    @Test
    @DisplayName("참여 중인 쪽지방에서 나간다")
    void leaveRoomSuccess() {
        ChatRoom room = room(100L);
        when(chatRoomRepository.findDetail(100L)).thenReturn(Optional.of(room));

        service.leaveRoom(USER_ID, 100L);

        assertThat(room.hasLeft(USER_ID)).isTrue();
    }

    @Test
    @DisplayName("이미 나간 쪽지방에서 다시 나가면 찾을 수 없는 방으로 처리한다")
    void leaveRoomFailsWhenAlreadyLeft() {
        ChatRoom room = room(100L);
        room.leave(USER_ID);
        when(chatRoomRepository.findDetail(100L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.leaveRoom(USER_ID, 100L))
                .isInstanceOf(ChatException.class)
                .extracting("baseResponseCode")
                .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    private ChatRoom room(Long roomId) {
        ChatRoom room = ChatRoom.builder()
                .contextType(ChatContextType.SESSION_SEARCH)
                .sender(user(USER_ID, "나"))
                .recipient(user(TARGET_USER_ID, "상대"))
                .sessionApplication(application(
                        APPLICATION_ID, TARGET_USER_ID, "상대 프로필"
                ))
                .build();
        ReflectionTestUtils.setField(room, "chatRoomId", roomId);
        return room;
    }

    private ChatRoom recruitmentRoom(Long roomId, Long recruitmentId) {
        User sender = user(USER_ID, "지원자");
        User recipient = user(TARGET_USER_ID, "밴드장");
        Band band = Band.builder()
                .owner(recipient)
                .name("테스트 밴드")
                .genre(Genre.HARD_ROCK)
                .region(Region.SEOUL)
                .build();
        SessionRecruitment recruitment = SessionRecruitment.builder()
                .sessionRecruitmentId(recruitmentId)
                .band(band)
                .build();
        ChatRoom room = ChatRoom.builder()
                .contextType(ChatContextType.RECRUITMENT)
                .sender(sender)
                .recipient(recipient)
                .sessionRecruitment(recruitment)
                .build();
        ReflectionTestUtils.setField(room, "chatRoomId", roomId);
        return room;
    }

    private SessionApplication application(Long id, Long userId, String nickname) {
        SessionApplication application = SessionApplication.builder()
                .userId(userId)
                .nickname(nickname)
                .title("세션 지원서")
                .purpose(DEFAULT_PURPOSE)
                .oneLineIntro("함께 연주해요")
                .part(Part.GUITAR)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .genre(Genre.HARD_ROCK)
                .region(Region.SEOUL)
                .intro("상세 소개")
                .build();
        ReflectionTestUtils.setField(
                application, "sessionApplicationId", id
        );
        return application;
    }

    private User user(Long id, String name) {
        return User.builder().id(id).name(name).build();
    }
}

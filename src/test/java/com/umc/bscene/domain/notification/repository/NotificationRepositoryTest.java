package com.umc.bscene.domain.notification.repository;

import com.umc.bscene.domain.notification.entity.Notification;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.config.JpaAuditingConfig;
import com.umc.bscene.global.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(JpaAuditingConfig.class)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findNotificationPage_읽지_않은_알림을_먼저_최신순으로_조회한다() {
        User user = saveUser();

        Notification oldRead = saveNotification(user, true);
        Notification oldUnread = saveNotification(user, false);
        Notification newRead = saveNotification(user, true);
        Notification newUnread = saveNotification(user, false);

        List<Notification> result =
                notificationRepository.findNotificationPage(
                        user.getId(),
                        null,
                        false,
                        PageRequest.ofSize(10)
                );

        assertThat(result)
                .extracting(Notification::getId)
                .containsExactly(
                        newUnread.getId(),
                        oldUnread.getId(),
                        newRead.getId(),
                        oldRead.getId()
                );
    }

    @Test
    void findNotificationPage_읽지_않은_커서_이후에는_ID가_큰_읽은_알림도_조회한다() {
        User user = saveUser();

        Notification oldRead = saveNotification(user, true);
        Notification unreadCursor = saveNotification(user, false);
        Notification newRead = saveNotification(user, true);

        List<Notification> result =
                notificationRepository.findNotificationPage(
                        user.getId(),
                        unreadCursor.getId(),
                        false,
                        PageRequest.ofSize(10)
                );

        assertThat(result)
                .extracting(Notification::getId)
                .containsExactly(
                        newRead.getId(),
                        oldRead.getId()
                );
    }

    @Test
    void findNotificationPage_읽은_커서_이후에는_더_오래된_읽은_알림만_조회한다() {
        User user = saveUser();

        Notification oldRead = saveNotification(user, true);
        saveNotification(user, false);
        Notification readCursor = saveNotification(user, true);
        saveNotification(user, false);

        List<Notification> result =
                notificationRepository.findNotificationPage(
                        user.getId(),
                        readCursor.getId(),
                        true,
                        PageRequest.ofSize(10)
                );

        assertThat(result)
                .extracting(Notification::getId)
                .containsExactly(oldRead.getId());
    }

    @Test
    void deleteByUserIdAndTypeAndReferenceId_같은_사용자의_같은_쪽지방_알림만_삭제한다() {
        User receiver = saveUser();
        User otherUser = saveUser("01087654321");

        Long targetChatRoomId = 10L;
        Long otherChatRoomId = 20L;

        Notification target = saveNotification(
                receiver,
                NotificationType.MESSAGE,
                targetChatRoomId
        );
        Notification otherRoomMessage = saveNotification(
                receiver,
                NotificationType.MESSAGE,
                otherChatRoomId
        );
        Notification otherType = saveNotification(
                receiver,
                NotificationType.POST,
                targetChatRoomId
        );
        Notification otherUserMessage = saveNotification(
                otherUser,
                NotificationType.MESSAGE,
                targetChatRoomId
        );

        long deletedCount =
                notificationRepository.deleteByUser_IdAndTypeAndReferenceId(
                        receiver.getId(),
                        NotificationType.MESSAGE,
                        targetChatRoomId
                );

        assertThat(deletedCount).isEqualTo(1L);

        assertThat(notificationRepository.findAll())
                .extracting(Notification::getId)
                .containsExactlyInAnyOrder(
                        otherRoomMessage.getId(),
                        otherType.getId(),
                        otherUserMessage.getId()
                )
                .doesNotContain(target.getId());
    }

    @Test
    void findNotificationPage_같은_쪽지방에서는_최신_알림만_조회한다() {
        User user = saveUser();

        Long firstChatRoomId = 10L;
        Long secondChatRoomId = 20L;

        Notification oldFirstRoomMessage = saveNotification(
                user,
                NotificationType.MESSAGE,
                firstChatRoomId
        );
        Notification secondRoomMessage = saveNotification(
                user,
                NotificationType.MESSAGE,
                secondChatRoomId
        );
        Notification newFirstRoomMessage = saveNotification(
                user,
                NotificationType.MESSAGE,
                firstChatRoomId
        );
        Notification postNotification = saveNotification(
                user,
                NotificationType.POST,
                firstChatRoomId
        );

        List<Notification> result =
                notificationRepository.findNotificationPage(
                        user.getId(),
                        null,
                        false,
                        PageRequest.ofSize(10)
                );

        assertThat(result)
                .extracting(Notification::getId)
                .containsExactly(
                        postNotification.getId(),
                        newFirstRoomMessage.getId(),
                        secondRoomMessage.getId()
                )
                .doesNotContain(oldFirstRoomMessage.getId());
    }

    @Test
    void existsByUserIdAndIsReadFalse_같은_방의_최신_쪽지를_읽었으면_과거_미확인_알림을_무시한다() {
        User user = saveUser();
        Long chatRoomId = 10L;

        saveNotification(
                user,
                NotificationType.MESSAGE,
                chatRoomId
        );

        Notification latestMessage = saveNotification(
                user,
                NotificationType.MESSAGE,
                chatRoomId
        );
        latestMessage.markAsRead();
        notificationRepository.flush();

        boolean hasUnread =
                notificationRepository.existsByUser_IdAndIsReadFalse(
                        user.getId()
                );

        assertThat(hasUnread).isFalse();
    }

    @Test
    void existsByUserIdAndIsReadFalse_같은_방의_최신_쪽지가_미확인이면_true를_반환한다() {
        User user = saveUser();
        Long chatRoomId = 10L;

        Notification oldMessage = saveNotification(
                user,
                NotificationType.MESSAGE,
                chatRoomId
        );
        oldMessage.markAsRead();

        saveNotification(
                user,
                NotificationType.MESSAGE,
                chatRoomId
        );

        notificationRepository.flush();

        boolean hasUnread =
                notificationRepository.existsByUser_IdAndIsReadFalse(
                        user.getId()
                );

        assertThat(hasUnread).isTrue();
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .name("알림테스트")
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .phone("01012345678")
                .build());
    }

    private Notification saveNotification(
            User user,
            boolean isRead
    ) {
        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.POST)
                .title("테스트 알림")
                .body("테스트 알림 내용")
                .deepLink("/test")
                .build();

        if (isRead) {
            notification.markAsRead();
        }

        return notificationRepository.saveAndFlush(notification);
    }

    private User saveUser(String phone) {
        return userRepository.save(User.builder()
                .name("다른사용자")
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .phone(phone)
                .build());
    }

    private Notification saveNotification(
            User user,
            NotificationType type,
            Long referenceId
    ) {
        return notificationRepository.saveAndFlush(
                Notification.builder()
                        .user(user)
                        .type(type)
                        .title("테스트 알림")
                        .body("테스트 알림 내용")
                        .deepLink("/test")
                        .referenceId(referenceId)
                        .build()
        );
    }
}

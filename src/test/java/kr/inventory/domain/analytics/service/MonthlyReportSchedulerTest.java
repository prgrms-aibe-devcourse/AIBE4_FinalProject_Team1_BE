package kr.inventory.domain.analytics.service;

import kr.inventory.domain.notification.service.publish.NotificationPublishCommand;
import kr.inventory.domain.notification.service.publish.NotificationPublishService;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import kr.inventory.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("월간 리포트 스케줄러 테스트")
class MonthlyReportSchedulerTest {

    @InjectMocks
    private MonthlyReportScheduler monthlyReportScheduler;

    @Mock
    private StoreMemberRepository storeMemberRepository;

    @Mock
    private NotificationPublishService notificationPublishService;

    @Nested
    @DisplayName("월간 리포트 스케줄 실행")
    class ScheduleMonthlyReport {

        @Test
        @DisplayName("ACTIVE 멤버들에게 월간 리포트 알림을 발송한다")
        void givenActiveMembers_whenScheduleMonthlyReport_thenSendNotifications() {
            // given
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            String expectedYearMonth = lastMonth.toString();

            User user1 = createUser(1L, "user1@test.com");
            User user2 = createUser(2L, "user2@test.com");
            Store store1 = createStore(UUID.randomUUID(), "매장1");
            Store store2 = createStore(UUID.randomUUID(), "매장2");

            StoreMember member1 = createStoreMember(1L, user1, store1, StoreMemberRole.OWNER, StoreMemberStatus.ACTIVE);
            StoreMember member2 = createStoreMember(2L, user2, store2, StoreMemberRole.MEMBER, StoreMemberStatus.ACTIVE);

            List<StoreMember> activeMembers = List.of(member1, member2);

            given(storeMemberRepository.findAllActiveWithUserAndStore()).willReturn(activeMembers);

            // when
            monthlyReportScheduler.scheduleMonthlyReport();

            // then
            verify(notificationPublishService, times(2)).publish(argThat(command ->
                command != null &&
                (command.userId().equals(1L) || command.userId().equals(2L)) &&
                command.metadata().get("yearMonth").asText().equals(expectedYearMonth)
            ));
        }

        @Test
        @DisplayName("ACTIVE 멤버가 없으면 알림을 발송하지 않는다")
        void givenNoActiveMembers_whenScheduleMonthlyReport_thenNoNotificationsSent() {
            // given
            given(storeMemberRepository.findAllActiveWithUserAndStore()).willReturn(Collections.emptyList());

            // when
            monthlyReportScheduler.scheduleMonthlyReport();

            // then
            verify(notificationPublishService, never()).publish(any());
        }

        @Test
        @DisplayName("알림 발송 중 예외가 발생해도 나머지 알림은 계속 발송된다")
        void givenExceptionDuringPublish_whenScheduleMonthlyReport_thenContinueSendingOthers() {
            // given
            User user1 = createUser(1L, "user1@test.com");
            User user2 = createUser(2L, "user2@test.com");
            User user3 = createUser(3L, "user3@test.com");
            Store store1 = createStore(UUID.randomUUID(), "매장1");
            Store store2 = createStore(UUID.randomUUID(), "매장2");
            Store store3 = createStore(UUID.randomUUID(), "매장3");

            StoreMember member1 = createStoreMember(1L, user1, store1, StoreMemberRole.OWNER, StoreMemberStatus.ACTIVE);
            StoreMember member2 = createStoreMember(2L, user2, store2, StoreMemberRole.MEMBER, StoreMemberStatus.ACTIVE);
            StoreMember member3 = createStoreMember(3L, user3, store3, StoreMemberRole.MEMBER, StoreMemberStatus.ACTIVE);

            List<StoreMember> activeMembers = List.of(member1, member2, member3);

            given(storeMemberRepository.findAllActiveWithUserAndStore()).willReturn(activeMembers);

            // 두 번째 알림에서 예외 발생
            given(notificationPublishService.publish(any(NotificationPublishCommand.class)))
                    .willReturn(1L)
                    .willThrow(new RuntimeException("Notification service error"))
                    .willReturn(3L);

            // when
            monthlyReportScheduler.scheduleMonthlyReport();

            // then
            // 예외가 발생해도 3번 모두 호출되어야 함
            verify(notificationPublishService, times(3)).publish(any());
        }

        @Test
        @DisplayName("전월 계산이 올바르게 동작한다")
        void givenCurrentMonth_whenScheduleMonthlyReport_thenUseLastMonth() {
            // given
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            String expectedYearMonth = lastMonth.toString();

            User user = createUser(1L, "user@test.com");
            Store store = createStore(UUID.randomUUID(), "테스트매장");
            StoreMember member = createStoreMember(1L, user, store, StoreMemberRole.OWNER, StoreMemberStatus.ACTIVE);

            given(storeMemberRepository.findAllActiveWithUserAndStore()).willReturn(List.of(member));

            // when
            monthlyReportScheduler.scheduleMonthlyReport();

            // then
            verify(notificationPublishService).publish(argThat(command ->
                command != null &&
                command.metadata().get("yearMonth").asText().equals(expectedYearMonth)
            ));
        }

        @Test
        @DisplayName("같은 사용자가 여러 매장에 속해있으면 각 매장별로 알림을 발송한다")
        void givenUserWithMultipleStores_whenScheduleMonthlyReport_thenSendNotificationForEachStore() {
            // given
            User user = createUser(1L, "user@test.com");
            Store store1 = createStore(UUID.randomUUID(), "매장1");
            Store store2 = createStore(UUID.randomUUID(), "매장2");

            StoreMember member1 = createStoreMember(1L, user, store1, StoreMemberRole.OWNER, StoreMemberStatus.ACTIVE);
            StoreMember member2 = createStoreMember(2L, user, store2, StoreMemberRole.OWNER, StoreMemberStatus.ACTIVE);

            List<StoreMember> activeMembers = List.of(member1, member2);

            given(storeMemberRepository.findAllActiveWithUserAndStore()).willReturn(activeMembers);

            // when
            monthlyReportScheduler.scheduleMonthlyReport();

            // then
            verify(notificationPublishService, times(2)).publish(argThat(command ->
                command != null &&
                command.userId().equals(1L) &&
                (command.metadata().get("storePublicId").asText().equals(store1.getStorePublicId().toString()) ||
                 command.metadata().get("storePublicId").asText().equals(store2.getStorePublicId().toString()))
            ));
        }
    }

    // 테스트 헬퍼 메서드
    private User createUser(Long userId, String email) {
        User user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(userId);
        lenient().when(user.getEmail()).thenReturn(email);
        return user;
    }

    private Store createStore(UUID storePublicId, String storeName) {
        Store store = mock(Store.class);
        lenient().when(store.getStorePublicId()).thenReturn(storePublicId);
        lenient().when(store.getName()).thenReturn(storeName);
        return store;
    }

    private StoreMember createStoreMember(Long storeMemberId, User user, Store store, StoreMemberRole role, StoreMemberStatus status) {
        StoreMember member = mock(StoreMember.class);
        lenient().when(member.getStoreMemberId()).thenReturn(storeMemberId);
        lenient().when(member.getUser()).thenReturn(user);
        lenient().when(member.getStore()).thenReturn(store);
        lenient().when(member.getRole()).thenReturn(role);
        lenient().when(member.getStatus()).thenReturn(status);
        return member;
    }
}
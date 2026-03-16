package kr.inventory.domain.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportScheduler {

    // TODO: 알림 연동 시 아래 의존성 주입 추가 필요
    // private final StoreRepository storeRepository;
    // private final StoreMemberRepository storeMemberRepository;
    // private final NotificationPublishService notificationPublishService;

    /**
     * 매월 1일 03:00 실행
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 1 * *")
    public void scheduleMonthlyReport() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        log.info("[MonthlyReportScheduler] {} 월간 리포트 스케줄러 시작", lastMonth);

        // TODO: 알림 연동
        // 전체 매장을 순회하며 ACTIVE 멤버에게 MONTHLY_OPS_REPORT_READY 알림 발송
        //
        // storeRepository.findAll().forEach(store -> {
        //     List<StoreMember> activeMembers = storeMemberRepository
        //         .findByStoreStoreIdAndStatus(store.getStoreId(), StoreMemberStatus.ACTIVE);
        //     activeMembers.forEach(member ->
        //         notificationPublishService.publish(
        //             NotificationPublishCommand.monthlyReportReady(
        //                 member.getUser().getUserId(),
        //                 store.getStorePublicId(),
        //                 lastMonth.toString()
        //             )
        //         )
        //     );
        // });

        log.info("[MonthlyReportScheduler] {} 월간 리포트 스케줄러 완료 (알림 연동 전)", lastMonth);
    }
}

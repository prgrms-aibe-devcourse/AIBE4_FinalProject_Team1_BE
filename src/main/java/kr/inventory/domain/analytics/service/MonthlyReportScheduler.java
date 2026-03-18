package kr.inventory.domain.analytics.service;

import kr.inventory.domain.notification.service.publish.NotificationPublishCommand;
import kr.inventory.domain.notification.service.publish.NotificationPublishService;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import kr.inventory.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportScheduler {

     private final StoreMemberRepository storeMemberRepository;
     private final NotificationPublishService notificationPublishService;

    /**
     * 매월 1일 03:00 실행 (KST)
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 1 * *", zone = "Asia/Seoul")
    public void scheduleMonthlyReport() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        log.info("[MonthlyReportScheduler] {} 월간 리포트 스케줄러 시작", lastMonth);

        // 전체 매장을 순회하며 ACTIVE 멤버에게 MONTHLY_OPS_REPORT_READY 알림 발송
        storeMemberRepository.findAllActiveWithUserAndStore()
            .forEach(member -> {
                try {
                    notificationPublishService.publish(
                            NotificationPublishCommand.monthlyReportReady(
                                    member.getUser().getUserId(),
                                    member.getStore().getStorePublicId(),
                                    lastMonth.toString()
                            )
                    );
                } catch (Exception e) {
                    log.error("[MonthlyReportScheduler] 알림 발송 실패 - userId: {}, storeId: {}",
                            member.getUser().getUserId(),
                            member.getStore().getStorePublicId(),
                            e);
                }
            });

        log.info("[MonthlyReportScheduler] {} 월간 리포트 스케줄러 완료", lastMonth);
    }
}

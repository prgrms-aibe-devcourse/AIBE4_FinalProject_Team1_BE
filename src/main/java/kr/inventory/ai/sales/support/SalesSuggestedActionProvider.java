package kr.inventory.ai.sales.support;

import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.sales.constant.SalesConstants;
import kr.inventory.ai.sales.tool.dto.response.SalesRecordItemToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SuggestedAction;
import kr.inventory.ai.sales.tool.support.SalesToolDateRange;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderDetailResponse;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class SalesSuggestedActionProvider {

    public List<SuggestedAction> buildSummaryFollowUps(SalesToolDateRange range) {
        String period = toPeriodPhrase(range);
        return List.of(
                new SuggestedAction("매출 추이 보기", period + " 매출 추이 보여줘"),
                new SuggestedAction("피크 시간 보기", period + " 매출 피크 시간대 알려줘"),
                new SuggestedAction("상위 메뉴 보기", period + " 상위 메뉴 보여줘")
        );
    }

    public List<SuggestedAction> buildTrendFollowUps(SalesToolDateRange range) {
        String period = toPeriodPhrase(range);
        return List.of(
                new SuggestedAction("매출 요약 보기", period + " 매출 요약해줘"),
                new SuggestedAction("피크 시간 보기", period + " 매출 피크 시간대 알려줘"),
                new SuggestedAction("상위 메뉴 보기", period + " 상위 메뉴 보여줘")
        );
    }

    public List<SuggestedAction> buildPeakFollowUps(SalesToolDateRange range) {
        String period = toPeriodPhrase(range);
        return List.of(
                new SuggestedAction("매출 요약 보기", period + " 매출 요약해줘"),
                new SuggestedAction("매출 추이 보기", period + " 매출 추이 보여줘"),
                new SuggestedAction("상위 메뉴 보기", period + " 상위 메뉴 보여줘")
        );
    }

    public List<SuggestedAction> buildTopMenuFollowUps(SalesToolDateRange range, String rankBy) {
        String period = toPeriodPhrase(range);
        String alternativeRankingPrompt = "amount".equals(rankBy)
                ? period + " 판매 수량 기준 상위 메뉴 보여줘"
                : period + " 매출 기준 상위 메뉴 보여줘";

        return List.of(
                new SuggestedAction("매출 요약 보기", period + " 매출 요약해줘"),
                new SuggestedAction("매출 추이 보기", period + " 매출 추이 보여줘"),
                new SuggestedAction("다른 기준 메뉴 보기", alternativeRankingPrompt)
        );
    }

    public List<SuggestedAction> buildRecordsFollowUps(SalesToolDateRange range, List<SalesRecordItemToolResponse> orders) {
        String period = toPeriodPhrase(range);
        if (orders.isEmpty()) {
            return List.of(
                    new SuggestedAction("환불 주문만 보기", period + " 환불 주문만 보여줘"),
                    new SuggestedAction("큰 주문 보기", period + " 주문을 금액 큰 순으로 보여줘")
            );
        }

        SalesRecordItemToolResponse first = orders.get(0);
        return List.of(
                new SuggestedAction("이 주문 상세 보기", "주문 번호 " + first.orderPublicId() + " 상세 보여줘"),
                new SuggestedAction("환불 주문만 보기", period + " 환불 주문만 보여줘"),
                new SuggestedAction("큰 주문 보기", period + " 주문을 금액 큰 순으로 보여줘")
        );
    }

    public List<SuggestedAction> buildOrderDetailFollowUps(SalesLedgerOrderDetailResponse detail) {
        String dayPhrase = toDayPhrase(detail.orderedAt());
        return List.of(
                new SuggestedAction("주문 기록 보기", dayPhrase + " 주문 기록 보여줘"),
                new SuggestedAction("환불 주문만 보기", dayPhrase + " 환불 주문만 보여줘"),
                new SuggestedAction("상위 메뉴 보기", dayPhrase + " 상위 메뉴 보여줘")
        );
    }

    public List<SuggestedAction> buildRefundFollowUps(SalesToolDateRange range) {
        String period = toPeriodPhrase(range);
        return List.of(
                new SuggestedAction("매출 요약 보기", period + " 매출 요약해줘"),
                new SuggestedAction("환불 주문 보기", period + " 환불 주문만 보여줘"),
                new SuggestedAction("매출 비교 보기", period + " 매출이 전 기간보다 어떤지 비교해줘")
        );
    }

    public List<SuggestedAction> buildMenuDetailFollowUps(SalesToolDateRange range, String menuName) {
        String period = toPeriodPhrase(range);
        return List.of(
                new SuggestedAction("이 메뉴가 포함된 주문 보기", period + " " + menuName + " 들어간 주문 보여줘"),
                new SuggestedAction("상위 메뉴 보기", period + " 상위 메뉴 보여줘"),
                new SuggestedAction("매출 요약 보기", period + " 매출 요약해줘")
        );
    }

    private String toPeriodPhrase(SalesToolDateRange range) {
        return DateRangePreset.toKoreanLabel(
                range.preset(),
                range.fromDate() + "부터 " + range.toDate() + "까지"
        );
    }

    private String toDayPhrase(OffsetDateTime orderedAt) {
        if (orderedAt == null) {
            return "최근";
        }
        return orderedAt.atZoneSameInstant(SalesConstants.KST).toLocalDate().toString();
    }
}

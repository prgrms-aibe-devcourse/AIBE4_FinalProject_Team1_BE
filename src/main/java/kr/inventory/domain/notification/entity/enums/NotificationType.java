package kr.inventory.domain.notification.entity.enums;

public enum NotificationType {

    // 재고 임계치(안전 재고) 미만
    INVENTORY_BELOW_THRESHOLD,

    // 유통기한 임박
    EXPIRATION_DUE_SOON,

    // AI 발주 추천 생성 완료
    AI_ORDER_RECOMMENDATION_READY,

    // 월간 운영 리포트 생성 완료
    MONTHLY_OPS_REPORT_READY
}
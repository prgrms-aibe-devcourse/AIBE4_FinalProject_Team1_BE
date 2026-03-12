package kr.inventory.domain.notification.entity.enums;

public enum NotificationType {

    // 재고가 안전 재고 임계치 미만으로 최초 진입했을 때 발생
    STOCK_BELOW_THRESHOLD,

    // 재고 차감/출고 처리 시 현재 재고가 부족하여 요청 수량을 모두 차감할 수 없을 때 발생
    STOCK_SHORTAGE_DETECTED,

    // 월간 운영 리포트 생성이 완료되어 사용자가 리포트를 확인할 수 있을 때 발생
    MONTHLY_OPS_REPORT_READY,

    // 사용자가 특정 매장 멤버로 등록된 직후, 가입 당사자에게 발송되는 알림
    STORE_MEMBER_REGISTERED,

    // 새 멤버가 특정 매장에 등록된 직후 기존 매장 대표(OWNER)에게 발송되는 알림
    STORE_MEMBER_JOINED
}
package kr.inventory.domain.sales.entity.enums;

public enum SalesOrderStatus {
    COMPLETED, // 주문 완료 (재고 차감은 비동기로 처리됨)
    REFUNDED   // 환불 완료
}

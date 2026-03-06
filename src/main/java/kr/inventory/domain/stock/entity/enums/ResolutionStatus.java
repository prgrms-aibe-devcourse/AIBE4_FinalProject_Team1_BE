package kr.inventory.domain.stock.entity.enums;


// 입고 아이템 재료 매핑 상태
public enum ResolutionStatus {

    PENDING, // 후보가 있으나 사용자 확정 필요 (중간 유사도)

    AUTO_RESOLVED, // 자동 매칭 완료 (매핑 테이블 확정 또는 높은 유사도)

    CONFIRMED, // 사용자가 수동으로 확정 완료

    FAILED // 매칭 실패 (후보 없음 또는 낮은 유사도), 사용자가 후보 선택 또는 새 재료 생성 필요
}

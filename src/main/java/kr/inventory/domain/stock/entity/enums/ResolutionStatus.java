package kr.inventory.domain.stock.entity.enums;


// 입고 아이템 재료 매핑 상태
public enum ResolutionStatus {

    AUTO_SUGGESTED, // 시스템이 재료를 자동 추천해서 값이 들어가 있는 상태, 사용자는 필요하면 수정 가능

    CONFIRMED, // 사용자가 직접 재료를 선택하거나 새 재료를 생성해서 연결한 상태

    FAILED // 현재 연결된 재료값이 없는 상태, 최종 확정 불가
}

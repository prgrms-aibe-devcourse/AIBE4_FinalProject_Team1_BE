package kr.inventory.domain.store.constant;

public final class NtsConstants {

    private NtsConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }

    // 국세청 사업자 상태 코드
    public static final String ACTIVE_BUSINESS_CODE = "01";  // 계속사업자
    public static final String SUSPENDED_BUSINESS_CODE = "02";  // 휴업자
    public static final String CLOSED_BUSINESS_CODE = "03";  // 폐업자
}

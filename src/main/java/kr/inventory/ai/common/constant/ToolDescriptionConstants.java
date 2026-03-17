package kr.inventory.ai.common.constant;

public final class ToolDescriptionConstants {

    private ToolDescriptionConstants() {
    }

    public static final String DATE_RANGE_PRESET = """
        period는 아래 값 중 하나만 사용합니다.
        - today
        - yesterday
        - this_week
        - this_month
        - last_7_days
        - last_30_days
        - last_month

        사용자 표현을 아래처럼 매핑합니다.
        - "오늘" -> today
        - "어제" -> yesterday
        - "이번 주", "금주" -> this_week
        - "이번 달", "이달" -> this_month
        - "최근 7일", "최근 일주일", "지난 7일" -> last_7_days
        - "최근 30일", "최근 한 달", "지난 30일" -> last_30_days
        - "지난달", "저번 달" -> last_month

        기간 언급이 없으면 period는 null입니다.
        """;
}
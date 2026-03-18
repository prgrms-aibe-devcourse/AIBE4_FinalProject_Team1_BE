package kr.inventory.ai.sales.constant;

import kr.inventory.domain.analytics.constant.SalesAnalyticsConstants;

import java.time.ZoneId;
import java.util.Locale;

public final class SalesConstants {

    // 날짜/시간 포맷
    public static final Locale DAY_LABEL_LOCALE = Locale.ENGLISH;
    public static final ZoneId KST = ZoneId.of(SalesAnalyticsConstants.TIMEZONE_KST);
}

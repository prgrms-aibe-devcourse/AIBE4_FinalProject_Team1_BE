package kr.inventory.ai.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum DateRangePreset {
    TODAY("today", "오늘"),
    YESTERDAY("yesterday", "어제"),
    THIS_WEEK("this_week", "이번 주"),
    THIS_MONTH("this_month", "이번 달"),
    LAST_7_DAYS("last_7_days", "최근 7일"),
    LAST_30_DAYS("last_30_days", "최근 30일"),
    LAST_MONTH("last_month", "지난달");

    private final String value;
    @Getter
    private final String koreanLabel;

    DateRangePreset(String value, String koreanLabel) {
        this.value = value;
        this.koreanLabel = koreanLabel;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DateRangePreset from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();

        return switch (normalized) {
            case "today", "오늘" -> TODAY;
            case "yesterday", "어제" -> YESTERDAY;
            case "this_week", "이번 주", "금주" -> THIS_WEEK;
            case "this_month", "이번 달", "이달" -> THIS_MONTH;
            case "last_7_days", "최근 7일", "최근 일주일", "지난 7일" -> LAST_7_DAYS;
            case "last_30_days", "최근 30일", "최근 한 달", "지난 30일" -> LAST_30_DAYS;
            case "last_month", "지난달", "저번 달" -> LAST_MONTH;
            default -> throw new IllegalArgumentException("Unknown DateRangePreset: " + value);
        };
    }

    public static String toKoreanLabel(String presetValue, String fallback) {
        if (presetValue == null) {
            return fallback;
        }

        try {
            DateRangePreset preset = from(presetValue);
            return preset != null ? preset.getKoreanLabel() : fallback;
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
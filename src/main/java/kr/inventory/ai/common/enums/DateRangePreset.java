package kr.inventory.ai.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DateRangePreset {
    TODAY("today"),
    YESTERDAY("yesterday"),
    THIS_WEEK("this_week"),
    THIS_MONTH("this_month"),
    LAST_7_DAYS("last_7_days"),
    LAST_30_DAYS("last_30_days"),
    LAST_MONTH("last_month");

    private final String value;

    DateRangePreset(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DateRangePreset from(String value) {
        if (value == null) {
            return null;
        }

        for (DateRangePreset preset : values()) {
            if (preset.value.equalsIgnoreCase(value)) {
                return preset;
            }
        }

        throw new IllegalArgumentException("Unknown DateRangePreset: " + value);
    }
}
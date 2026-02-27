package kr.inventory.global.util;

// 사업자등록번호 정규화 및 포맷팅 유틸리티
public class BusinessRegistrationNumberUtil {

    private static final int NORMALIZED_LENGTH = 10;

    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("사업자등록번호는 필수입니다.");
        }

        // 숫자만 추출
        String digitsOnly = input.replaceAll("[^0-9]", "");

        // 10자리 검증
        if (digitsOnly.length() != NORMALIZED_LENGTH) {
            throw new IllegalArgumentException(
                String.format("사업자등록번호는 %d자리 숫자여야 합니다. (입력: %s)",
                    NORMALIZED_LENGTH, input)
            );
        }

        return digitsOnly;
    }

    public static String format(String normalized) {
        if (normalized == null || normalized.length() != NORMALIZED_LENGTH) {
            return normalized;
        }

        return String.format("%s-%s-%s",
            normalized.substring(0, 3),
            normalized.substring(3, 5),
            normalized.substring(5, 10)
        );
    }

    public static boolean isNormalized(String value) {
        if (value == null || value.length() != NORMALIZED_LENGTH) {
            return false;
        }
        return value.matches("^[0-9]{10}$");
    }
}

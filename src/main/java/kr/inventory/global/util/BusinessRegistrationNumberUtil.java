package kr.inventory.global.util;

/**
 * 사업자등록번호 정규화 및 포맷팅 유틸리티
 *
 * <p>저장 규칙: 숫자만 10자리 (예: 2088203460)
 * <p>표시 규칙: 하이픈 포함 (예: 208-82-03460)
 */
public class BusinessRegistrationNumberUtil {

    private static final int NORMALIZED_LENGTH = 10;

    /**
     * 사업자등록번호 정규화
     * - 하이픈 제거
     * - 숫자만 추출
     * - 10자리 검증
     *
     * @param input 입력된 사업자등록번호 (208-82-03460 또는 2088203460)
     * @return 정규화된 사업자등록번호 (2088203460)
     * @throws IllegalArgumentException 유효하지 않은 형식인 경우
     */
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

    /**
     * 사업자등록번호 포맷팅
     * - 2088203460 → 208-82-03460
     *
     * @param normalized 정규화된 사업자등록번호 (10자리 숫자)
     * @return 포맷팅된 사업자등록번호 (208-82-03460)
     */
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

    /**
     * 사업자등록번호 유효성 검증 (정규화 가능 여부)
     *
     * @param input 입력된 사업자등록번호
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public static boolean isValid(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        String digitsOnly = input.replaceAll("[^0-9]", "");
        return digitsOnly.length() == NORMALIZED_LENGTH;
    }

    /**
     * 정규화된 사업자등록번호인지 확인
     *
     * @param value 확인할 값
     * @return 10자리 숫자이면 true
     */
    public static boolean isNormalized(String value) {
        if (value == null || value.length() != NORMALIZED_LENGTH) {
            return false;
        }
        return value.matches("^[0-9]{10}$");
    }
}

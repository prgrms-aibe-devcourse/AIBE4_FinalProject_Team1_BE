package kr.inventory.global.util;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnitConverter {

	public static String convertToStandardUnit(String raw) {
		if (raw == null || raw.isBlank())
			return null;
		if (raw.length() > 50)
			return null; // 길이 제한은 여전히 유효

		String cleaned = raw.toLowerCase().replaceAll("\\s", "");

		// 1. 숫자와 단위가 시작되는 지점 찾기
		int unitStartIndex = -1;
		for (int i = 0; i < cleaned.length(); i++) {
			char c = cleaned.charAt(i);
			// 숫자가 아닌 문자가 처음 등장하는 지점 탐색
			if (!Character.isDigit(c) && c != '.') {
				unitStartIndex = i;
				break;
			}
		}

		try {
			// 2. 숫자만 있는 경우
			if (unitStartIndex == -1) {
				return parseToIntegerString(cleaned);
			}

			// 3. 숫자부와 단위부 분리
			String numericPart = cleaned.substring(0, unitStartIndex);
			String unitPart = cleaned.substring(unitStartIndex);

			if (numericPart.isEmpty())
				return null;
			double value = Double.parseDouble(numericPart);

			// 4. 단위별 환산 (정규식 없이 단순 문자열 비교)
			return switch (unitPart) {
				case "kg", "l", "킬로", "리터", "키로" -> String.valueOf((int)(value * 1000));
				case "g", "ml", "그램", "밀리", "밀리리터" -> String.valueOf((int)value);
				default -> String.valueOf((int)value);
			};
		} catch (Exception e) {
			return null;
		}
	}

	private static String parseToIntegerString(String numericStr) {
		try {
			return String.valueOf((int)Double.parseDouble(numericStr));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
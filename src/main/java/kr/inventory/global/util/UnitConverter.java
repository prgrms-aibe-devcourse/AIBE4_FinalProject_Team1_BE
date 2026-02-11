package kr.inventory.global.util;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnitConverter {

	private static final Pattern UNIT_PATTERN =
		Pattern.compile("([0-9.]++)\\s*+([a-zA-Z가-힣]++)");

	public static String convertToStandardUnit(String raw) {
		if (raw == null || raw.isBlank())
			return null;

		if (raw.length() > 100)
			return null;

		String cleaned = raw.toLowerCase().replaceAll("\\s", "");
		Matcher matcher = UNIT_PATTERN.matcher(cleaned);

		if (matcher.find()) {
			try {
				double value = Double.parseDouble(matcher.group(1));
				String unit = matcher.group(2);

				return switch (unit) {
					// 한글 단위 지원 확장
					case "kg", "l", "킬로", "리터", "키로" -> String.valueOf((int)(value * 1000));
					case "g", "ml", "그램", "밀리", "밀리리터" -> String.valueOf((int)value);
					default -> String.valueOf((int)value);
				};
			} catch (NumberFormatException e) {
				return null;
			}
		}

		String numericOnly = cleaned.replaceAll("[^0-9.]", "");
		return numericOnly.isEmpty() ? null : String.valueOf((int)Double.parseDouble(numericOnly));
	}
}

package kr.inventory.domain.stock.normalization.constant;

import java.util.Set;
import java.util.regex.Pattern;

public final class InboundItemResolutionConstants {

    public static final double AUTO_SCORE_THRESHOLD = 0.85;
    public static final double AUTO_GAP_THRESHOLD = 0.25;
    public static final int TOP_N_CANDIDATES = 5;

    public static final Set<String> STOP_WORDS = Set.of(
            // 법인/회사 표기
            "주식회사", "유한회사", "회사", "주",
            "the", "co", "ltd", "inc", "corp",

            // 원산지/수식어
            "국내산", "국산", "수입", "수입산",
            "프리미엄", "건강", "자연방사", "무항생제",
            "난각번호",

            // 포장/묶음
            "묶음", "번들", "박스", "상자", "망", "봉", "팩", "포장", "개별포장",
            "케이스", "case", "box",
            "단", "통", "캔", "병",

            // 형태/운영 표현
            "업소용"
    );

    public static final Set<String> KEY_DESCRIPTION_STOP_WORDS = Set.of(
            "두꺼운", "얇은", "큰", "작은",
            "개봉", "개봉후", "보관", "보관용"
    );

    public static final Pattern KEY_USAGE_SUFFIX = Pattern.compile(".+용$");

    public static final Set<String> STORAGE_TOKENS_FOR_KEY = Set.of(
            "냉장", "냉동", "실온", "상온",
            "chilled", "frozen"
    );

    // 괄호/대괄호 컨텐츠 추출
    public static final Pattern BRACKET_CONTENT = Pattern.compile("\\[([^\\]]+)]");
    public static final Pattern PAREN_CONTENT = Pattern.compile("\\(([^\\)]+)\\)");

    // 기본 정규화용 패턴
    public static final Pattern DELIMS_TO_SPACE = Pattern.compile("[,()\\[\\]{}\\/<>]");
    public static final Pattern KEEP_ALLOWED = Pattern.compile("[^a-z0-9가-힣\\s]");
    public static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    // 숫자/단위 판단
    public static final Set<String> UNIT_TOKENS = Set.of(
            "kg", "g", "l", "ml", "ea",
            "개", "p", "입", "구", "매", "장", "호", "번",
            "단", "망", "박스", "봉", "팩", "통", "캔", "병"
    );

    public static final Pattern NUMBER_TOKEN = Pattern.compile("^\\d+(?:[.,]\\d+)?$");
    public static final Pattern COMBINED_QTY_TOKEN = Pattern.compile(
            "^\\d+(?:[.,]\\d+)?(kg|g|l|ml|ea|개|p|입|구|매|장|호|번|단|망|박스|봉|팩|통|캔|병)$",
            Pattern.CASE_INSENSITIVE
    );

    public static final Pattern PROMOTION_PACK_TOKEN =
            Pattern.compile("^\\d+[x*+]\\d+$|^\\d+개입$", Pattern.CASE_INSENSITIVE);
}
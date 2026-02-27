package kr.inventory.global.config.swagger;

public final class PurchaseApiDocs {

    private PurchaseApiDocs() {
    }

    public static final String CREATE_DRAFT = """
        발주서 초안을 생성합니다.

        입력 기준:
        - storeId: 발주 대상 매장 ID
        - vendorPublicId: 매장에 속한 활성 거래처 공개 ID
        - items: 발주 품목 목록(최소 1개)

        동작:
        - DRAFT 상태로 생성됩니다.
        - 품목 기준으로 총금액이 계산됩니다.
        """;

    public static final String LIST = """
        매장 기준 발주서 목록을 조회합니다.

        응답 요약:
        - 최신 등록 순으로 반환합니다.
        - 주문번호, 상태, 총금액, 거래처 정보가 포함됩니다.
        """;

    public static final String DETAIL = """
        발주서 상세 정보를 조회합니다.

        포함 정보:
        - 거래처/품목 정보
        - 상태 정보 및 상태 변경 이력(제출/확정/취소)
        """;

    public static final String UPDATE_DRAFT = """
        발주서 초안을 수정합니다.

        수정 규칙:
        - DRAFT 상태에서만 수정할 수 있습니다.
        - 거래처와 품목 목록을 함께 갱신합니다.
        - 갱신된 품목 기준으로 총금액을 재계산합니다.
        """;

    public static final String SUBMIT = """
        발주서를 제출 상태로 변경합니다.

        처리 내용:
        - DRAFT -> SUBMITTED 상태 전환
        - 주문번호, 제출자, 제출 시각 기록
        """;

    public static final String CONFIRM = """
        제출된 발주서를 확정합니다.

        권한/상태 규칙:
        - OWNER 권한만 확정할 수 있습니다.
        - SUBMITTED 상태에서만 확정됩니다.
        - 확정자와 확정 시각을 저장합니다.
        """;

    public static final String CANCEL = """
        발주서를 취소 상태로 변경합니다.

        처리 내용:
        - 취소 가능한 상태에서만 취소됩니다.
        - 취소자와 취소 시각을 저장합니다.
        """;

    public static final String DOWNLOAD_PDF = """
        발주서 PDF 파일을 다운로드합니다.

        동작:
        - 권한 검증 후 발주서 내용을 PDF로 생성합니다.
        - 생성된 파일을 첨부 다운로드로 반환합니다.
        """;
}

package kr.inventory.domain.store.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StoreErrorCode implements ErrorModel {

    // 매장
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "매장을 찾을 수 없습니다."),
    DUPLICATE_BUSINESS_REGISTRATION_NUMBER(HttpStatus.CONFLICT, "S002", "이미 등록된 사업자등록번호입니다."),
    INVALID_BUSINESS_REGISTRATION_NUMBER(HttpStatus.BAD_REQUEST, "S003", "유효하지 않은 사업자등록번호입니다."),
    CLOSED_BUSINESS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "S004", "폐업 상태의 사업자는 매장을 등록할 수 없습니다."),
    STORE_NOT_FOUND_OR_ACCESS_DENIED(HttpStatus.FORBIDDEN, "STORE_403", "해당 매장에 대한 접근 권한이 없거나 존재하지 않는 매장입니다."),

    // 멤버
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "SM001", "멤버를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND_IN_STORE(HttpStatus.NOT_FOUND, "SM002", "해당 매장에서 멤버를 찾을 수 없습니다."),
    CANNOT_DEACTIVATE_OWNER(HttpStatus.BAD_REQUEST, "SM003", "매장 소유자는 비활성화할 수 없습니다."),

    // 초대
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "초대를 찾을 수 없습니다."),
    INVITATION_EXPIRED(HttpStatus.BAD_REQUEST, "I002", "초대가 만료되었습니다."),
    INVITATION_REVOKED(HttpStatus.BAD_REQUEST, "I003", "취소된 초대입니다."),
    ALREADY_MEMBER(HttpStatus.BAD_REQUEST, "I004", "이미 해당 매장의 멤버입니다."),
    CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "I005", "초대 코드 생성에 실패했습니다."),
    INVALID_INVITATION_REQUEST(HttpStatus.BAD_REQUEST, "I007", "초대 수락 요청이 올바르지 않습니다. token 또는 code 중 하나만 입력해야 합니다."),
    NO_ACTIVE_INVITATION(HttpStatus.NOT_FOUND, "I008", "활성화된 초대가 없습니다."),

    // 권한
    NOT_STORE_MEMBER(HttpStatus.FORBIDDEN, "S101", "해당 매장의 멤버가 아닙니다."),
    OWNER_PERMISSION_REQUIRED(HttpStatus.FORBIDDEN, "S103", "매장 소유자만 수행할 수 있는 작업입니다."),

    // 국세청 API(NTS)
    NTS_API_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "S201", "국세청 API 연결에 실패했습니다."),
    NTS_API_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "S202", "국세청 API 서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

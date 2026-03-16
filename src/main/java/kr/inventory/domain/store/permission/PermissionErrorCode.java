package kr.inventory.domain.store.permission;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PermissionErrorCode implements ErrorModel {

    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "P001", "해당 작업을 수행할 권한이 없습니다."),
    PERMISSION_CONTEXT_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "P002", "권한 검증 컨텍스트를 해석할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

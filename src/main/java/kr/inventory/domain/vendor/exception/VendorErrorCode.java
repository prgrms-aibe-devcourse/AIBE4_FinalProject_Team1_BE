package kr.inventory.domain.vendor.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VendorErrorCode implements ErrorModel {

    VENDOR_DUPLICATE_NAME(HttpStatus.CONFLICT, "V001","이미 존재하는 거래처명입니다"),
    VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND, "V002","거래처를 찾을 수 없습니다"),
    VENDOR_INACTIVE(HttpStatus.BAD_REQUEST, "V003","비활성화된 거래처입니다"),
    VENDOR_ACCESS_DENIED(HttpStatus.FORBIDDEN, "V004","거래처 접근 권한이 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

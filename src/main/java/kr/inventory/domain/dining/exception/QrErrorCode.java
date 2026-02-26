package kr.inventory.domain.dining.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum QrErrorCode implements ErrorModel {
    QR_NOT_FOUND(HttpStatus.NOT_FOUND, "QR_001", "해당 QR코드가 등록되어있지 않습니다.,"),
    QR_IMAGE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "QR_002", "QR 이미지 생성 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

package kr.inventory.domain.notification.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class NotificationException extends BusinessException {
    public NotificationException(ErrorModel errorModel) {
        super(errorModel);
    }
}

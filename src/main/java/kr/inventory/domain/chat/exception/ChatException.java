package kr.inventory.domain.chat.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class ChatException extends BusinessException {
    public ChatException(ErrorModel errorModel) {
        super(errorModel);
    }
}

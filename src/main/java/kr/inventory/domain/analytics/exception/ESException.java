package kr.inventory.domain.analytics.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class ESException extends BusinessException {
	public ESException(ErrorModel errorModel) {
		super(errorModel);
	}
}

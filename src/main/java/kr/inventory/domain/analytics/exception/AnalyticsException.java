package kr.inventory.domain.analytics.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;

public class AnalyticsException extends BusinessException {
    public AnalyticsException(ErrorModel errorModel) {
        super(errorModel);
    }
}
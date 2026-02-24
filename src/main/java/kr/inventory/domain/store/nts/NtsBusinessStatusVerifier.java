package kr.inventory.domain.store.nts;

import kr.inventory.domain.store.constant.NtsConstants;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.nts.dto.NtsBusinessStatusResponse;
import kr.inventory.domain.store.service.BusinessStatusVerifier;
import kr.inventory.global.util.BusinessRegistrationNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NtsBusinessStatusVerifier implements BusinessStatusVerifier {

    private final NtsApiClient ntsApiClient;

    @Override
    public void verify(String businessRegistrationNumber) {
        // 정규화된 값을 받지만, 안전을 위해 재검증
        if (!BusinessRegistrationNumberUtil.isNormalized(businessRegistrationNumber)) {
            throw new StoreException(StoreErrorCode.INVALID_BUSINESS_REGISTRATION_NUMBER);
        }

        NtsBusinessStatusResponse response;
        try {
            response = ntsApiClient.getBusinessStatus(businessRegistrationNumber);
        } catch (NtsApiClientException e) {
            throw mapToStoreException(e);
        }

        NtsBusinessStatusResponse.DataItem dataItem = extractFirstItemOrThrow(response);

        if (NtsConstants.CLOSED_BUSINESS_CODE.equals(dataItem.bSttCd())) {
            throw new StoreException(StoreErrorCode.CLOSED_BUSINESS_NOT_ALLOWED);
        }
    }

    private NtsBusinessStatusResponse.DataItem extractFirstItemOrThrow(NtsBusinessStatusResponse response) {
        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new StoreException(StoreErrorCode.INVALID_BUSINESS_REGISTRATION_NUMBER);
        }
        NtsBusinessStatusResponse.DataItem dataItem = response.data().get(0);
        if (dataItem == null || dataItem.bSttCd() == null) {
            throw new StoreException(StoreErrorCode.INVALID_BUSINESS_REGISTRATION_NUMBER);
        }
        return dataItem;
    }

    private StoreException mapToStoreException(NtsApiClientException e) {
        return switch (e.getType()) {
            case SERVER_ERROR -> new StoreException(StoreErrorCode.NTS_API_SERVER_ERROR);
            case CONNECTION_ERROR -> new StoreException(StoreErrorCode.NTS_API_CONNECTION_ERROR);
            case API_KEY_ERROR -> new StoreException(StoreErrorCode.NTS_API_SERVER_ERROR);
            case RATE_LIMIT_ERROR -> new StoreException(StoreErrorCode.NTS_API_SERVER_ERROR);
            case INVALID_RESPONSE -> new StoreException(StoreErrorCode.INVALID_BUSINESS_REGISTRATION_NUMBER);
            case CLIENT_ERROR -> mapClientError(e.getStatusCode());
            default -> new StoreException(StoreErrorCode.NTS_API_CONNECTION_ERROR);
        };
    }

    private StoreException mapClientError(HttpStatusCode statusCode) {
        if (statusCode != null && statusCode.value() == 400) {
            return new StoreException(StoreErrorCode.INVALID_BUSINESS_REGISTRATION_NUMBER);
        }
        return new StoreException(StoreErrorCode.NTS_API_SERVER_ERROR);
    }
}

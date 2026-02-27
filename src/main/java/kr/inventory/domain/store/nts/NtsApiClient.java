package kr.inventory.domain.store.nts;

import kr.inventory.domain.store.nts.dto.NtsBusinessStatusRequest;
import kr.inventory.domain.store.nts.dto.NtsBusinessStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class NtsApiClient {

    private final RestClient ntsRestClient;
    private final String ntsApiKey;

    public NtsApiClient(
            RestClient ntsRestClient,
            @Value("${nts.api.key}") String ntsApiKey
    ) {
        this.ntsRestClient = ntsRestClient;
        this.ntsApiKey = ntsApiKey;
    }

    public NtsBusinessStatusResponse getBusinessStatus(String businessNumber) {
        try {
            NtsBusinessStatusRequest request = NtsBusinessStatusRequest.of(businessNumber);

            NtsBusinessStatusResponse response = ntsRestClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/status")
                    .queryParam("serviceKey", ntsApiKey)
                    .build())
                .body(request)
                .retrieve()
                .body(NtsBusinessStatusResponse.class);

            validateResponse(response, businessNumber);
            return response;

        } catch (HttpClientErrorException e) {
            throw handleClientError(e, businessNumber);
        } catch (HttpServerErrorException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            log.error("NTS API 5xx: businessNumber={}, status={}, body={}",
                businessNumber, statusCode, e.getResponseBodyAsString(), e);

            throw new NtsApiClientException(
                NtsApiClientException.Type.SERVER_ERROR,
                statusCode,
                "NTS API server error",
                e
            );
        } catch (ResourceAccessException e) {
            log.error("NTS API connection error: businessNumber={}", businessNumber, e);
            throw new NtsApiClientException(
                NtsApiClientException.Type.CONNECTION_ERROR,
                "NTS API connection error",
                e
            );
        } catch (Exception e) {
            log.error("NTS API unknown error: businessNumber={}", businessNumber, e);
            throw new NtsApiClientException(
                NtsApiClientException.Type.UNKNOWN,
                "NTS API unknown error",
                e
            );
        }
    }

    private void validateResponse(NtsBusinessStatusResponse response, String businessNumber) {
        if (response == null) {
            log.error("NTS API returned null response: businessNumber={}", businessNumber);
            throw new NtsApiClientException(
                NtsApiClientException.Type.INVALID_RESPONSE,
                "NTS API returned null response",
                null
            );
        }

        if (!"OK".equals(response.statusCode())) {
            log.error("NTS API invalid status code: businessNumber={}, statusCode={}",
                businessNumber, response.statusCode());
            throw new NtsApiClientException(
                NtsApiClientException.Type.INVALID_RESPONSE,
                "NTS API returned invalid status code: " + response.statusCode(),
                null
            );
        }
    }

    private NtsApiClientException handleClientError(HttpClientErrorException e, String businessNumber) {
        HttpStatusCode statusCode = e.getStatusCode();
        int statusValue = statusCode.value();

        log.error("NTS API 4xx: businessNumber={}, status={}, body={}",
            businessNumber, statusCode, e.getResponseBodyAsString(), e);

        NtsApiClientException.Type errorType = switch (statusValue) {
            case 401, 403 -> NtsApiClientException.Type.API_KEY_ERROR;
            case 429 -> NtsApiClientException.Type.RATE_LIMIT_ERROR;
            case 400 -> NtsApiClientException.Type.CLIENT_ERROR;
            default -> NtsApiClientException.Type.CLIENT_ERROR;
        };

        return new NtsApiClientException(
            errorType,
            statusCode,
            "NTS API client error: " + statusValue,
            e
        );
    }
}

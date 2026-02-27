package kr.inventory.domain.store.nts;

import org.springframework.http.HttpStatusCode;

public class NtsApiClientException extends RuntimeException {

    public enum Type {
        CLIENT_ERROR,
        SERVER_ERROR,
        CONNECTION_ERROR,
        INVALID_RESPONSE,
        API_KEY_ERROR,
        RATE_LIMIT_ERROR,
        UNKNOWN
    }

    private final Type type;
    private final HttpStatusCode statusCode;

    public NtsApiClientException(Type type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.statusCode = null;
    }

    public NtsApiClientException(Type type, HttpStatusCode statusCode, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.statusCode = statusCode;
    }

    public Type getType() {
        return type;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}

package kr.dontworry.domain.auth.constant;

import kr.dontworry.domain.auth.exception.AuthErrorCode;
import kr.dontworry.domain.auth.exception.AuthException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OAuthProvider {
    GOOGLE("google"),
    KAKAO("kakao");

    private final String registrationId;

    public static OAuthProvider from(String registrationId) {
        for (OAuthProvider provider : values()) {
            if (provider.registrationId.equalsIgnoreCase(registrationId)) {
                return provider;
            }
        }
        throw new AuthException(AuthErrorCode.UNSUPPORTED_PROVIDER);
    }
}
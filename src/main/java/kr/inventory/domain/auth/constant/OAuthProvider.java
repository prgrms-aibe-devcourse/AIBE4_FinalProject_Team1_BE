package kr.inventory.domain.auth.constant;

import kr.inventory.domain.auth.exception.AuthErrorCode;
import kr.inventory.domain.auth.exception.AuthException;
import kr.inventory.domain.user.entity.enums.SocialProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OAuthProvider {
    GOOGLE("google", SocialProvider.GOOGLE),
    KAKAO("kakao", SocialProvider.KAKAO);

    private final String registrationId;
    private final SocialProvider socialProvider;

    public static OAuthProvider from(String registrationId) {
        for (OAuthProvider provider : values()) {
            if (provider.registrationId.equalsIgnoreCase(registrationId)) {
                return provider;
            }
        }
        throw new AuthException(AuthErrorCode.UNSUPPORTED_PROVIDER);
    }
}
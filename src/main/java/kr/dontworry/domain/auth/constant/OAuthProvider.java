package kr.dontworry.domain.auth.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OAuthProvider {
    GOOGLE("google"),
    KAKAO("kakao");

    private final String registrationId;
}
package kr.inventory.domain.auth.security;

import kr.inventory.domain.auth.constant.OAuthProvider;

import java.util.Map;

public record KakaoUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {
    @Override public String getProvider() { return OAuthProvider.KAKAO.getRegistrationId(); }
    @Override public String getProviderId() { return attributes.get("id").toString(); }
    @Override public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        return account != null ? (String) account.get("email") : null;
    }
    @Override public String getName() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        return properties != null ? (String) properties.get("nickname") : null;
    }
}

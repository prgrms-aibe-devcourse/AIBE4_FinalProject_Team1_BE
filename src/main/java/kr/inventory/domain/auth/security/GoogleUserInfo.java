package kr.inventory.domain.auth.security;

import kr.inventory.domain.auth.constant.OAuthProvider;

import java.util.Map;

public record GoogleUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {
    @Override public String getProvider() { return OAuthProvider.GOOGLE.getRegistrationId(); }
    @Override public String getProviderId() { return (String) attributes.get("sub"); }
    @Override public String getEmail() { return (String) attributes.get("email"); }
    @Override public String getName() { return (String) attributes.get("name"); }
}

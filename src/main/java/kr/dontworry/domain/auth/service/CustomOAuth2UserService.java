package kr.dontworry.domain.auth.service;

import kr.dontworry.domain.auth.constant.OAuthProvider;
import kr.dontworry.domain.auth.dto.CustomUserDetails;
import kr.dontworry.domain.auth.dto.GoogleUserInfo;
import kr.dontworry.domain.auth.dto.KakaoUserInfo;
import kr.dontworry.domain.auth.dto.OAuth2UserInfo;
import kr.dontworry.domain.user.entity.SocialAccount;
import kr.dontworry.domain.user.entity.User;
import kr.dontworry.domain.user.entity.UserRole;
import kr.dontworry.domain.user.repository.SocialAccountRepository;
import kr.dontworry.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService
        extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = createOAuth2UserInfo(registrationId, oauth2User.getAttributes());
        User user = getOrSaveUser(registrationId, userInfo);

        return new CustomUserDetails(user.getId(), List.of(new SimpleGrantedAuthority(UserRole.USER.getKey())), oauth2User.getAttributes());
    }

    private OAuth2UserInfo createOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        OAuthProvider provider = OAuthProvider.from(registrationId);

        return switch (provider) {
            case GOOGLE -> new GoogleUserInfo(attributes);
            case KAKAO -> new KakaoUserInfo(attributes);
        };
    }

    private User getOrSaveUser(String registrationId, OAuth2UserInfo userInfo) {
        return socialAccountRepository.findByProviderAndProviderId(registrationId, userInfo.getProviderId())
                .map(SocialAccount::getUser)
                .orElseGet(() -> createNewSocialUser(registrationId, userInfo));
    }

    private User createNewSocialUser(String registrationId, OAuth2UserInfo userInfo) {
        User user = userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> userRepository.save(new User(userInfo.getEmail(), userInfo.getName())));

        socialAccountRepository.save(new SocialAccount(registrationId, userInfo.getProviderId(), user));

        return user;
    }
}
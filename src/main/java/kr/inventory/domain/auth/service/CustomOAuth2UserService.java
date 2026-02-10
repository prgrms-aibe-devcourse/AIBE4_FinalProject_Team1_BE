package kr.inventory.domain.auth.service;

import kr.inventory.domain.auth.constant.OAuthProvider;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.auth.security.GoogleUserInfo;
import kr.inventory.domain.auth.security.KakaoUserInfo;
import kr.inventory.domain.auth.security.OAuth2UserInfo;
import kr.inventory.domain.user.entity.SocialLogin;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.SocialLoginRepository;
import kr.inventory.domain.user.repository.UserRepository;
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
    private final SocialLoginRepository socialLoginRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthProvider oAuthProvider = OAuthProvider.from(registrationId);

        OAuth2UserInfo userInfo = createOAuth2UserInfo(oAuthProvider, oauth2User.getAttributes());
        User user = getOrSaveUser(oAuthProvider, userInfo);

        return new CustomUserDetails(
                user.getUserId(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                oauth2User.getAttributes()
        );
    }

    private OAuth2UserInfo createOAuth2UserInfo(OAuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> new GoogleUserInfo(attributes);
            case KAKAO -> new KakaoUserInfo(attributes);
        };
    }

    private User getOrSaveUser(OAuthProvider oAuthProvider, OAuth2UserInfo userInfo) {
        return socialLoginRepository.findByProviderAndProviderId(oAuthProvider.getSocialProvider(), userInfo.getProviderId())
                .map(SocialLogin::getUser)
                .orElseGet(() -> createNewSocialUser(oAuthProvider, userInfo));
    }

    private User createNewSocialUser(OAuthProvider oAuthProvider, OAuth2UserInfo userInfo) {
        User user = userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> userRepository.save(User.create(userInfo.getName(), userInfo.getEmail())));

        socialLoginRepository.save(SocialLogin.create(user, oAuthProvider.getSocialProvider(), userInfo.getProviderId()));

        return user;
    }
}
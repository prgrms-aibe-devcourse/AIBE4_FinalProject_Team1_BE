package kr.dontworry.auth.service;

import kr.dontworry.auth.dto.CustomOAuth2User;
import kr.dontworry.auth.dto.GoogleUserInfo;
import kr.dontworry.auth.dto.KakaoUserInfo;
import kr.dontworry.auth.dto.OAuth2UserInfo;
import kr.dontworry.user.entity.SocialAccount;
import kr.dontworry.user.entity.User;
import kr.dontworry.user.repository.SocialAccountRepository;
import kr.dontworry.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        OAuth2UserInfo userInfo = switch (registrationId) {
            case "google" -> new GoogleUserInfo(oauth2User.getAttributes());
            case "kakao" -> new KakaoUserInfo(oauth2User.getAttributes());
            default -> throw new RuntimeException("지원하지 않는 제공자입니다.");
        };

        String email = userInfo.getEmail();

        User user = socialAccountRepository.findByProviderAndProviderId(registrationId, userInfo.getProviderId())
                .map(SocialAccount::getUser)
                .orElseGet(() -> {
                    User existingUser = userRepository.findByEmail(email)
                            .orElseGet(() -> userRepository.save(new User(email)));

                    socialAccountRepository.save(new SocialAccount(registrationId, userInfo.getProviderId(), existingUser));
                    return existingUser;
                });

        return new CustomOAuth2User(user, oauth2User.getAttributes());
    }
}
package kr.inventory.global.util;

import kr.inventory.domain.auth.exception.AuthErrorCode;
import kr.inventory.domain.auth.exception.AuthException;
import kr.inventory.global.auth.constant.AuthConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeaderUtilTest {

    private final HeaderUtil headerUtil = new HeaderUtil();
    private final String testToken = "test-access-token";

    @Test
    @DisplayName("setAccessTokenHeader: 기존 HttpHeaders 객체에 Bearer 토큰 헤더를 추가한다")
    void setAccessTokenHeader_Success() {
        // given
        HttpHeaders headers = new HttpHeaders();

        // when
        headerUtil.setAccessTokenHeader(headers, testToken);

        // then
        String authHeader = headers.getFirst(AuthConstant.AUTHORIZATION_HEADER);
        assertThat(authHeader).isEqualTo(AuthConstant.BEARER_PREFIX + testToken);
    }

    @Test
    @DisplayName("createAccessTokenHeaders: 새로운 HttpHeaders 객체를 생성하고 토큰을 설정하여 반환한다")
    void createAccessTokenHeaders_Success() {
        // when
        HttpHeaders headers = headerUtil.createAccessTokenHeaders(testToken);

        // then
        assertThat(headers).isNotNull();
        String authHeader = headers.getFirst(AuthConstant.AUTHORIZATION_HEADER);
        assertThat(authHeader).isEqualTo(AuthConstant.BEARER_PREFIX + testToken);
    }

    @Test
    @DisplayName("extractToken: Bearer 접두사가 포함된 헤더에서 토큰만 정확히 추출한다")
    void extractToken_Success() {
        // given
        String bearerToken = AuthConstant.BEARER_PREFIX + testToken;

        // when
        String result = headerUtil.extractToken(bearerToken);

        // then
        assertThat(result).isEqualTo(testToken);
    }

    @Test
    @DisplayName("extractToken: Bearer 접두사가 없거나 형식이 틀리면 AuthException이 발생한다")
    void extractToken_Fail_InvalidFormat() {
        // given
        String invalidToken = "Basic " + testToken;

        // when & then
        assertThatThrownBy(() -> headerUtil.extractToken(invalidToken))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException authException = (AuthException) ex;
                    assertThat(authException.getErrorModel()).isEqualTo(AuthErrorCode.INVALID_TOKEN_FORMAT);
                });
    }

    @Test
    @DisplayName("extractToken: 헤더 값이 null이거나 비어있으면 AuthException(INVALID_TOKEN_FORMAT)이 발생한다")
    void extractToken_Fail_Empty() {
        assertThatThrownBy(() -> headerUtil.extractToken(null))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException authException = (AuthException) ex;
                    assertThat(authException.getErrorModel()).isEqualTo(AuthErrorCode.INVALID_TOKEN_FORMAT);
                });

        assertThatThrownBy(() -> headerUtil.extractToken(""))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException authException = (AuthException) ex;
                    assertThat(authException.getErrorModel()).isEqualTo(AuthErrorCode.INVALID_TOKEN_FORMAT);
                });
    }
}
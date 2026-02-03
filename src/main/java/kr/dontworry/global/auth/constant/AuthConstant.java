package kr.dontworry.global.auth.constant;

public class AuthConstant {
    private AuthConstant() {}

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    public static final String AUTH_CLAIM_KEY = "auth";
    public static final String SID_CLAIM_KEY = "sid";

    public static final String OAUTH_REDIRECT_PATH = "/oauth/redirect";
    public static final String REDIRECT_PARAM_CODE = "code";
}

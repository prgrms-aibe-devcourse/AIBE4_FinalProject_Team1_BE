package kr.inventory.global.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.inventory.global.auth.constant.AuthConstant;
import kr.inventory.global.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthorizationRedirectFilter extends OncePerRequestFilter {

    private final CookieUtil cookieUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri == null || !requestUri.startsWith("/oauth2/authorization/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String redirectUri = request.getParameter(AuthConstant.REDIRECT_URI_PARAM);

        if (redirectUri != null && !redirectUri.isBlank()) {
            cookieUtil.addOauthRedirectUriCookie(response, redirectUri);
        }

        filterChain.doFilter(request, response);
    }
}
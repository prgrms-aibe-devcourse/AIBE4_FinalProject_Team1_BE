package kr.inventory.global.auth.util;

import java.security.Principal;
import kr.inventory.domain.auth.exception.AuthErrorCode;
import kr.inventory.domain.auth.exception.AuthException;
import kr.inventory.domain.auth.security.CustomUserDetails;
import org.springframework.security.core.Authentication;

public final class PrincipalUtils {

    private PrincipalUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Long extractUserId(Principal principal) {
        if (principal instanceof Authentication authentication) {
            Object authenticationPrincipal = authentication.getPrincipal();
            if (authenticationPrincipal instanceof CustomUserDetails userDetails) {
                return userDetails.getUserId();
            }
        }

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }

        if (principal == null || principal.getName() == null) {
            throw new AuthException(AuthErrorCode.USER_INFO_NOT_FOUND);
        }

        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            throw new AuthException(AuthErrorCode.INVALID_USER_ID_FORMAT);
        }
    }
}

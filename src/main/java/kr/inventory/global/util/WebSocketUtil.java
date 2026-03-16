package kr.inventory.global.util;

import java.security.Principal;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.global.constant.WebSocketConstants;
import org.springframework.security.core.Authentication;

public final class WebSocketUtil {
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
            throw new IllegalStateException(WebSocketConstants.ERROR_NO_USER_INFO);
        }

        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(WebSocketConstants.ERROR_INVALID_USER_ID, e);
        }
    }
}

package kr.inventory.domain.user.controller;

import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.user.controller.dto.UserProfileResponse;
import kr.inventory.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal) {

        UserProfileResponse response = userService.getUserProfile(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    // MCP 서버와의 테스트를 위한 임시 함수
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable(name = "userId") Long userId) {

        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }
}
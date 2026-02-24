package kr.inventory.domain.user.controller;

import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.user.controller.dto.UserProfileResponse;
import kr.inventory.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}
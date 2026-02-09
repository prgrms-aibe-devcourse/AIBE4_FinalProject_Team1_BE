package kr.inventory.domain.auth.controller.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}

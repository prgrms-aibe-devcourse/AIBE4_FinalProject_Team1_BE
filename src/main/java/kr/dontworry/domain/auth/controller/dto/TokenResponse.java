package kr.dontworry.domain.auth.controller.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}

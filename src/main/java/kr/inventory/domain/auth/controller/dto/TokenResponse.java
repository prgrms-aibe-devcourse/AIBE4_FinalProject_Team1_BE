package kr.inventory.domain.auth.controller.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
    public static TokenResponse from(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken);
    }
}
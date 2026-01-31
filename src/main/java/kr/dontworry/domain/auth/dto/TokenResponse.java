package kr.dontworry.domain.auth.dto;

public record TokenResponse(String accessToken, String refreshToken) {
}

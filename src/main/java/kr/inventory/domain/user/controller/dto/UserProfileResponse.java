package kr.inventory.domain.user.controller.dto;

public record UserProfileResponse(
        String name,
        String email
) {
    public static UserProfileResponse from(String name, String email) {
        return new UserProfileResponse(name, email);
    }
}
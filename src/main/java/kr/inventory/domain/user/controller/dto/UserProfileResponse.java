package kr.inventory.domain.user.controller.dto;

public record UserProfileResponse(
        String name,
        String email
) {
}
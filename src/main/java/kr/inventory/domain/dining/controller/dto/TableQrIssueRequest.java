package kr.inventory.domain.dining.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TableQrIssueRequest(
        @NotNull UUID tablePublicId
) {}

package kr.inventory.domain.dining.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record TableQrsIssueRequest(
        @NotNull List<UUID> tablePublicIds
) {}

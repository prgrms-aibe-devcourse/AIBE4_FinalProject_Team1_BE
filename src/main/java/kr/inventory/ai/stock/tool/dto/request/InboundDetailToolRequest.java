package kr.inventory.ai.stock.tool.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InboundDetailToolRequest(
        @NotNull
        UUID inboundPublicId
) {
}

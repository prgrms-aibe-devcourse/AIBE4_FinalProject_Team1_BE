package kr.inventory.domain.stock.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ManualInboundRequest(
	UUID vendorPublicId,

	@NotNull
	LocalDate inboundDate,

	@NotEmpty
	@Valid
	List<ManualInboundItemRequest> items
) {
}

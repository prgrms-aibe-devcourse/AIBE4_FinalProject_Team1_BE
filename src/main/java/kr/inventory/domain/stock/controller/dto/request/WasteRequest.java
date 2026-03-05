package kr.inventory.domain.stock.controller.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kr.inventory.domain.stock.entity.enums.WasteReason;

public record WasteRequest(
	@NotEmpty(message = "폐기 항목이 최소 하나는 있어야 합니다")
	@Valid
	List<WasteItem> items
) {
	public record WasteItem(
		UUID stockBatchId,
		@NotNull(message = "폐기 수량은 필수입니다.")
		@Positive(message = "폐기 수량은 0보다 커야 합니다.")
		BigDecimal quantity,

		@NotNull(message = "폐기 사유는 필수입니다.")
		WasteReason reason,

		@NotNull(message = "폐기 일자는 필수입니다")
		OffsetDateTime wasteDate
	) {
	}
}



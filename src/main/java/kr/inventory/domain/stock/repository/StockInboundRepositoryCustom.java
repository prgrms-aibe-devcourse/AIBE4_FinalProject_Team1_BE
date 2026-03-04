package kr.inventory.domain.stock.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import kr.inventory.domain.stock.controller.dto.response.StockInboundResponse;
import kr.inventory.domain.stock.entity.StockInbound;

public interface StockInboundRepositoryCustom {
	Optional<StockInboundResponse> findInboundWithItems(UUID inboundPublicId, Long storeId);
}

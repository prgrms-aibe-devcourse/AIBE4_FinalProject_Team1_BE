package kr.inventory.domain.stock.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import kr.inventory.domain.stock.controller.dto.request.StockInboundSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockInboundResponse;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.enums.InboundStatus;

import java.util.List;

public interface StockInboundRepositoryCustom {
	Page<StockInbound> searchInbounds(
			Long storeId,
			List<InboundStatus> statuses,
			StockInboundSearchRequest searchRequest,
			Pageable pageable
	);
}

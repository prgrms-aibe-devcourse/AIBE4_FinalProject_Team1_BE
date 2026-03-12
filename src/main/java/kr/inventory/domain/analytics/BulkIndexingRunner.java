package kr.inventory.domain.analytics;

import kr.inventory.domain.analytics.document.sales.SalesOrderDocument;
import kr.inventory.domain.analytics.document.stock.IngredientStockBatchDocument;
import kr.inventory.domain.analytics.document.stock.StockInboundDocument;
import kr.inventory.domain.analytics.document.stock.StockLogDocument;
import kr.inventory.domain.analytics.document.stock.WasteRecordDocument;
import kr.inventory.domain.analytics.repository.SalesOrderSearchRepository;
import kr.inventory.domain.analytics.repository.StockBatchSearchRepository;
import kr.inventory.domain.analytics.repository.StockInboundSearchRepository;
import kr.inventory.domain.analytics.repository.StockLogSearchRepository;
import kr.inventory.domain.analytics.repository.WasteRecordSearchRepository;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockLog;
import kr.inventory.domain.stock.entity.WasteRecord;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StockInboundRepository;
import kr.inventory.domain.stock.repository.StockLogRepository;
import kr.inventory.domain.stock.repository.WasteRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 애플리케이션 기동 시 기존 DB 데이터를 Elasticsearch에 bulk 인덱싱.
 * - dev 프로파일에서만 실행 (운영 환경 재기동 시 불필요한 재인덱싱 방지)
 * - 이미 ES에 데이터가 있는 경우 중복 저장되지 않도록 count 체크 후 스킵
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class BulkIndexingRunner implements ApplicationRunner {

	private static final int BATCH_SIZE = 500;

	private final SalesOrderRepository salesOrderRepository;
	private final SalesOrderItemRepository salesOrderItemRepository;
	private final StockInboundRepository stockInboundRepository;
	private final StockLogRepository stockLogRepository;
	private final WasteRecordRepository wasteRecordRepository;
	private final IngredientStockBatchRepository ingredientStockBatchRepository;

	private final SalesOrderSearchRepository salesOrderSearchRepository;
	private final StockInboundSearchRepository stockInboundSearchRepository;
	private final StockLogSearchRepository stockLogSearchRepository;
	private final WasteRecordSearchRepository wasteRecordSearchRepository;
	private final StockBatchSearchRepository stockBatchSearchRepository;

	@Override
	@Transactional(readOnly = true)
	public void run(ApplicationArguments args) {
		log.info("[ES] BulkIndexingRunner 시작");
		bulkIndexSalesOrders();
		bulkIndexStockInbounds();
		bulkIndexStockLogs();
		bulkIndexWasteRecords();
		bulkIndexStockBatches();
		log.info("[ES] BulkIndexingRunner 완료");
	}

	// 주문
	private void bulkIndexSalesOrders() {
		long esCount = salesOrderSearchRepository.count();
		if (esCount > 0) {
			log.info("[ES] sales_orders 인덱스에 데이터 존재({}건), 스킵", esCount);
			return;
		}

		log.info("[ES] sales_orders bulk 인덱싱 시작");
		int page = 0;
		int total = 0;

		while (true) {
			Page<SalesOrder> pageResult = salesOrderRepository.findByStatus(
				SalesOrderStatus.COMPLETED,
				PageRequest.of(page, BATCH_SIZE)
			);

			List<SalesOrder> orders = pageResult.getContent();
			if (orders.isEmpty()) {
				break;
			}

			List<Long> orderIds = orders.stream()
				.map(SalesOrder::getSalesOrderId)
				.toList();

			List<SalesOrderItem> allItems = salesOrderItemRepository
				.findBySalesOrderSalesOrderIdIn(orderIds);

			Map<Long, List<SalesOrderItem>> itemsByOrderId = allItems.stream()
				.collect(Collectors.groupingBy(
					item -> item.getSalesOrder().getSalesOrderId()
				));

			List<SalesOrderDocument> docs = orders.stream()
				.map(order -> SalesOrderDocument.from(
					order,
					itemsByOrderId.getOrDefault(order.getSalesOrderId(), List.of())
				))
				.toList();

			salesOrderSearchRepository.saveAll(docs);
			total += docs.size();
			log.info("[ES] sales_orders {}건 인덱싱 완료 (누적 {}건)", docs.size(), total);

			if (!pageResult.hasNext()) {
				break;
			}
			page++;
		}

		log.info("[ES] sales_orders 총 {}건 인덱싱 완료", total);
	}

	// 입고
	private void bulkIndexStockInbounds() {
		long esCount = stockInboundSearchRepository.count();
		if (esCount > 0) {
			log.info("[ES] stock_inbounds 인덱스에 데이터 존재({}건), 스킵", esCount);
			return;
		}

		log.info("[ES] stock_inbounds bulk 인덱싱 시작");
		int page = 0;
		int total = 0;

		while (true) {
			Page<StockInbound> pageResult = stockInboundRepository.findByStatus(
				InboundStatus.CONFIRMED,
				PageRequest.of(page, BATCH_SIZE)
			);

			List<StockInbound> inbounds = pageResult.getContent();
			if (inbounds.isEmpty()) {
				break;
			}

			List<StockInboundDocument> docs = inbounds.stream()
				.map(StockInboundDocument::from)
				.toList();

			stockInboundSearchRepository.saveAll(docs);
			total += docs.size();
			log.info("[ES] stock_inbounds {}건 인덱싱 완료 (누적 {}건)", docs.size(), total);

			if (!pageResult.hasNext()) {
				break;
			}
			page++;
		}

		log.info("[ES] stock_inbounds 총 {}건 인덱싱 완료", total);
	}

	// 입고 로그
	private void bulkIndexStockLogs() {
		long esCount = stockLogSearchRepository.count();
		if (esCount > 0) {
			log.info("[ES] stock_logs 인덱스에 데이터 존재({}건), 스킵", esCount);
			return;
		}

		log.info("[ES] stock_logs bulk 인덱싱 시작");
		int page = 0;
		int total = 0;

		while (true) {
			Page<StockLog> pageResult = stockLogRepository.findAll(
				PageRequest.of(page, BATCH_SIZE)
			);

			List<StockLog> logs = pageResult.getContent();
			if (logs.isEmpty()) {
				break;
			}

			List<StockLogDocument> docs = logs.stream()
				.map(StockLogDocument::from)
				.toList();

			stockLogSearchRepository.saveAll(docs);
			total += docs.size();
			log.info("[ES] stock_logs {}건 인덱싱 완료 (누적 {}건)", docs.size(), total);

			if (!pageResult.hasNext()) {
				break;
			}
			page++;
		}

		log.info("[ES] stock_logs 총 {}건 인덱싱 완료", total);
	}

	// 폐기
	private void bulkIndexWasteRecords() {
		long esCount = wasteRecordSearchRepository.count();
		if (esCount > 0) {
			log.info("[ES] waste_records 인덱스에 데이터 존재({}건), 스킵", esCount);
			return;
		}

		log.info("[ES] waste_records bulk 인덱싱 시작");
		int page = 0;
		int total = 0;

		while (true) {
			Page<WasteRecord> pageResult = wasteRecordRepository.findAll(
				PageRequest.of(page, BATCH_SIZE)
			);

			List<WasteRecord> records = pageResult.getContent();
			if (records.isEmpty()) {
				break;
			}

			List<WasteRecordDocument> docs = records.stream()
				.map(WasteRecordDocument::from)
				.toList();

			wasteRecordSearchRepository.saveAll(docs);
			total += docs.size();
			log.info("[ES] waste_records {}건 인덱싱 완료 (누적 {}건)", docs.size(), total);

			if (!pageResult.hasNext()) {
				break;
			}
			page++;
		}

		log.info("[ES] waste_records 총 {}건 인덱싱 완료", total);
	}

	private void bulkIndexStockBatches() {
		long esCount = stockBatchSearchRepository.count();
		if (esCount > 0) {
			log.info("[ES] stock_batches 인덱스에 데이터 존재({}건), 스킵", esCount);
			return;
		}

		log.info("[ES] stock_batches bulk 인덱싱 시작");
		int page = 0;
		int total = 0;

		while (true) {
			// QueryDSL로 구현한 Fetch Join 메서드 호출
			Page<IngredientStockBatch> pageResult = ingredientStockBatchRepository.findAll(
				PageRequest.of(page, BATCH_SIZE)
			);

			List<IngredientStockBatch> batches = pageResult.getContent();
			if (batches.isEmpty())
				break;

			List<IngredientStockBatchDocument> docs = batches.stream()
				.map(batch -> IngredientStockBatchDocument.from(
					batch,
					batch.getIngredient().getLowStockThreshold()
				))
				.toList();

			stockBatchSearchRepository.saveAll(docs);

			log.info("[ES] StockBatch {}건 인덱싱 완료", docs.size());
			if (!pageResult.hasNext())
				break;
			page++;
		}
		log.info("[ES] stock_batches 총 {}건 인덱싱 완료", total);
	}

}

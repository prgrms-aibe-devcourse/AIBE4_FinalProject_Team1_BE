package kr.inventory.domain.sales.repository;

import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.TableSession;
import kr.inventory.domain.dining.repository.DiningTableRepository;
import kr.inventory.domain.dining.repository.TableSessionRepository;
import kr.inventory.domain.sales.controller.dto.response.SalesOrderResponse;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import kr.inventory.domain.sales.repository.impl.SalesOrderRepositoryImpl;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(SalesOrderRepositoryImpl.class)
@DisplayName("주문 리포지토리 테스트")
class SalesOrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private DiningTableRepository diningTableRepository;

    @Autowired
    private TableSessionRepository tableSessionRepository;

    private Store store;
    private DiningTable table;
    private TableSession session;

    @BeforeEach
    void setUp() {
        // Store 생성
        store = Store.create("테스트 매장", "1234567890");
        storeRepository.save(store);

        // DiningTable 생성
        table = DiningTable.create(store, "T1");
        diningTableRepository.save(table);

        // TableSession 생성
        session = TableSession.create(
                table,
                null,
                "hashed-token",
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(2)
        );
        tableSessionRepository.save(session);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Idempotency Key로 주문 조회")
    void givenIdempotencyKey_whenFindByStoreStoreIdAndIdempotencyKey_thenReturnOrder() {
        // given
        String idempotencyKey = "idempotency-123";
        SalesOrder order = SalesOrder.create(store, table, session, idempotencyKey, SalesOrderType.DINE_IN);
        salesOrderRepository.save(order);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<SalesOrder> found = salesOrderRepository
                .findByStoreStoreIdAndIdempotencyKey(store.getStoreId(), idempotencyKey);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getIdempotencyKey()).isEqualTo(idempotencyKey);
    }

    @Test
    @DisplayName("OrderPublicId로 주문 조회 (주문 항목 포함)")
    void givenOrderPublicId_whenFindByOrderPublicIdWithItems_thenReturnOrderWithItems() {
        // given
        SalesOrder order = SalesOrder.create(store, table, session, "idempotency-123", SalesOrderType.DINE_IN);
        salesOrderRepository.save(order);

        // 주문 항목 추가 (Menu 없이 직접 생성)
        // SalesOrderItem item1 = new SalesOrderItem();
        // item1 필드 설정 필요 (실제로는 Menu가 필요하므로 스킵)

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<SalesOrder> found = salesOrderRepository
                .findByOrderPublicIdWithItems(order.getOrderPublicId(), store.getStoreId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getOrderPublicId()).isEqualTo(order.getOrderPublicId());
    }

    @Test
    @DisplayName("매장 주문 목록 조회 (최신순)")
    void givenStoreId_whenFindStoreOrders_thenReturnOrdersDescending() {
        // given
        SalesOrder order1 = SalesOrder.create(store, table, session, "idempotency-1", SalesOrderType.DINE_IN);
        order1.updateStatus(SalesOrderStatus.COMPLETED);
        salesOrderRepository.save(order1);

        // 1초 후 주문
        SalesOrder order2 = SalesOrder.create(store, table, session, "idempotency-2", SalesOrderType.DINE_IN);
        order2.updateStatus(SalesOrderStatus.COMPLETED);
        salesOrderRepository.save(order2);

        entityManager.flush();
        entityManager.clear();

        // when
        List<SalesOrderResponse> responses = salesOrderRepository.findStoreOrders(store.getStoreId());

        // then
        assertThat(responses).hasSize(2);
        // 최신순 정렬 확인
        assertThat(responses.get(0).orderPublicId()).isEqualTo(order2.getOrderPublicId());
        assertThat(responses.get(1).orderPublicId()).isEqualTo(order1.getOrderPublicId());
    }

    @Test
    @DisplayName("주문 목록 조회 - 다른 매장 주문 제외")
    void givenMultipleStores_whenFindStoreOrders_thenReturnOnlyTargetStoreOrders() {
        // given
        Store otherStore = Store.create("다른 매장", "9876543210");
        storeRepository.save(otherStore);

        DiningTable otherTable = DiningTable.create(otherStore, "T2");
        diningTableRepository.save(otherTable);

        // 첫 번째 매장 주문
        SalesOrder order1 = SalesOrder.create(store, table, session, "idempotency-1", SalesOrderType.DINE_IN);
        salesOrderRepository.save(order1);

        // 두 번째 매장 주문
        SalesOrder order2 = SalesOrder.create(otherStore, otherTable, null, "idempotency-2", SalesOrderType.DINE_IN);
        salesOrderRepository.save(order2);

        entityManager.flush();
        entityManager.clear();

        // when
        List<SalesOrderResponse> responses = salesOrderRepository.findStoreOrders(store.getStoreId());

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).orderPublicId()).isEqualTo(order1.getOrderPublicId());
    }

    @Test
    @DisplayName("OrderPublicId와 StoreId로 주문 조회")
    void givenOrderPublicIdAndStoreId_whenFindByOrderPublicIdAndStoreStoreId_thenReturnOrder() {
        // given
        SalesOrder order = SalesOrder.create(store, table, session, "idempotency-123", SalesOrderType.DINE_IN);
        salesOrderRepository.save(order);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<SalesOrder> found = salesOrderRepository
                .findByOrderPublicIdAndStoreStoreId(order.getOrderPublicId(), store.getStoreId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getOrderPublicId()).isEqualTo(order.getOrderPublicId());
        assertThat(found.get().getStore().getStoreId()).isEqualTo(store.getStoreId());
    }
}

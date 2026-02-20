package kr.inventory.domain.purchase.service;

import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderItemRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderUpdateRequest;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderItemResponse;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.PurchaseOrderItem;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.purchase.exception.PurchaseOrderErrorCode;
import kr.inventory.domain.purchase.exception.PurchaseOrderException;
import kr.inventory.domain.purchase.factory.PurchaseOrderFactory;
import kr.inventory.domain.purchase.mapper.PurchaseOrderResponseMapper;
import kr.inventory.domain.purchase.repository.PurchaseOrderRepository;
import kr.inventory.domain.purchase.service.impl.PurchaseOrderServiceImpl;
import kr.inventory.domain.purchase.validator.PurchaseOrderValidator;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private PurchaseOrderPdfService purchaseOrderPdfService;

    @Mock
    private PurchaseOrderFactory purchaseOrderFactory;

    @Mock
    private PurchaseOrderNumberGenerator purchaseOrderNumberGenerator;

    @Mock
    private PurchaseOrderValidator purchaseOrderValidator;

    @Mock
    private PurchaseOrderResponseMapper purchaseOrderResponseMapper;

    @InjectMocks
    private PurchaseOrderServiceImpl purchaseOrderService;

    @Test
    @DisplayName("MANAGER 이상은 DRAFT 발주서를 생성하고 수정할 수 있다")
    void createAndUpdateDraft_success() {
        // given
        Long userId = 1L;
        Store store = createStore(10L);

        PurchaseOrderCreateRequest createRequest = new PurchaseOrderCreateRequest(
                store.getStoreId(),
                List.of(new PurchaseOrderItemRequest("onion", 2, new BigDecimal("1000")))
        );

        PurchaseOrder savedOrder = PurchaseOrder.createDraft(store);
        ReflectionTestUtils.setField(savedOrder, "purchaseOrderId", 100L);
        savedOrder.replaceItems(List.of(PurchaseOrderItem.create("onion", 2, new BigDecimal("1000"))));

        PurchaseOrder createdDraft = PurchaseOrder.createDraft(store);
        createdDraft.replaceItems(List.of(PurchaseOrderItem.create("onion", 2, new BigDecimal("1000"))));
        List<PurchaseOrderItem> updatedItems = List.of(PurchaseOrderItem.create("garlic", 3, new BigDecimal("1200")));

        when(storeRepository.findById(store.getStoreId())).thenReturn(Optional.of(store));
        when(purchaseOrderValidator.requireManagerOrAbove(any(), any())).thenReturn(StoreMemberRole.MANAGER);
        when(purchaseOrderFactory.createDraft(any(Store.class), any())).thenReturn(createdDraft);
        when(purchaseOrderFactory.createItems(any())).thenReturn(updatedItems);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(savedOrder);
        when(purchaseOrderRepository.findWithItemsByPurchaseOrderId(savedOrder.getPurchaseOrderId())).thenReturn(Optional.of(savedOrder));
        when(purchaseOrderResponseMapper.toDetailResponse(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> toDetailResponse(invocation.getArgument(0)));

        // when
        PurchaseOrderDetailResponse created = purchaseOrderService.createDraft(userId, createRequest);
        PurchaseOrderDetailResponse updated = purchaseOrderService.updateDraft(
                userId,
                savedOrder.getPurchaseOrderId(),
                new PurchaseOrderUpdateRequest(List.of(new PurchaseOrderItemRequest("garlic", 3, new BigDecimal("1200"))))
        );

        // then
        assertThat(created.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(updated.items()).hasSize(1);
        assertThat(updated.items().get(0).itemName()).isEqualTo("garlic");
    }

    @Test
    @DisplayName("DRAFT가 아닌 발주서는 수정할 수 없다")
    void updateDraft_failWhenNotDraft() {
        // given
        Long userId = 2L;
        Store store = createStore(11L);

        PurchaseOrder order = PurchaseOrder.createDraft(store);
        ReflectionTestUtils.setField(order, "purchaseOrderId", 101L);
        order.replaceItems(List.of(PurchaseOrderItem.create("tomato", 1, new BigDecimal("3000"))));
        order.submit("PO-20260220-000101", userId, java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));

        when(purchaseOrderRepository.findWithItemsByPurchaseOrderId(order.getPurchaseOrderId())).thenReturn(Optional.of(order));
        when(purchaseOrderValidator.requireManagerOrAbove(any(), any())).thenReturn(StoreMemberRole.MANAGER);
        doThrow(new PurchaseOrderException(PurchaseOrderErrorCode.DRAFT_ONLY_MUTATION))
                .when(purchaseOrderValidator)
                .requireDraftForUpdate(any());

        assertThatThrownBy(() -> purchaseOrderService.updateDraft(
                userId,
                order.getPurchaseOrderId(),
                new PurchaseOrderUpdateRequest(List.of(new PurchaseOrderItemRequest("rice", 1, new BigDecimal("5000"))))
        )).isInstanceOf(PurchaseOrderException.class)
                .extracting(exception -> ((PurchaseOrderException) exception).getErrorModel())
                .isEqualTo(PurchaseOrderErrorCode.DRAFT_ONLY_MUTATION);
    }

    @Test
    @DisplayName("제출 시 orderNo와 제출자/제출시간이 저장된다")
    void submit_success() {
        // given
        Long userId = 3L;
        Store store = createStore(12L);
        PurchaseOrder order = PurchaseOrder.createDraft(store);
        ReflectionTestUtils.setField(order, "purchaseOrderId", 202L);
        order.replaceItems(List.of(PurchaseOrderItem.create("milk", 5, new BigDecimal("2000"))));

        when(purchaseOrderRepository.findWithItemsByPurchaseOrderId(order.getPurchaseOrderId())).thenReturn(Optional.of(order));
        when(purchaseOrderValidator.requireManagerOrAbove(any(), any())).thenReturn(StoreMemberRole.MANAGER);
        when(purchaseOrderNumberGenerator.generate(any(), any())).thenReturn("PO-20260220-000202");
        when(purchaseOrderResponseMapper.toDetailResponse(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> toDetailResponse(invocation.getArgument(0)));

        // when
        PurchaseOrderDetailResponse submitted = purchaseOrderService.submit(userId, order.getPurchaseOrderId());

        // then
        assertThat(submitted.status()).isEqualTo(PurchaseOrderStatus.SUBMITTED);
        assertThat(submitted.orderNo()).startsWith("PO-");
        assertThat(submitted.submittedByUserId()).isEqualTo(userId);
        assertThat(submitted.submittedAt()).isNotNull();
    }

    @Test
    @DisplayName("확정은 OWNER만 가능하다")
    void confirm_ownerOnly() {
        // given
        Long userId = 4L;
        Store store = createStore(13L);
        PurchaseOrder order = PurchaseOrder.createDraft(store);
        ReflectionTestUtils.setField(order, "purchaseOrderId", 303L);
        order.replaceItems(List.of(PurchaseOrderItem.create("pepper", 4, new BigDecimal("700"))));
        order.submit("PO-20260220-000303", 99L, java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));

        when(purchaseOrderRepository.findWithItemsByPurchaseOrderId(order.getPurchaseOrderId())).thenReturn(Optional.of(order));
        when(purchaseOrderValidator.requireManagerOrAbove(any(), any()))
                .thenReturn(StoreMemberRole.MANAGER)
                .thenReturn(StoreMemberRole.OWNER);
        doThrow(new PurchaseOrderException(PurchaseOrderErrorCode.PURCHASE_ORDER_ACCESS_DENIED))
                .when(purchaseOrderValidator)
                .requireOwner(StoreMemberRole.MANAGER);
        when(purchaseOrderResponseMapper.toDetailResponse(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> toDetailResponse(invocation.getArgument(0)));

        assertThatThrownBy(() -> purchaseOrderService.confirm(userId, order.getPurchaseOrderId()))
                .isInstanceOf(PurchaseOrderException.class)
                .extracting(exception -> ((PurchaseOrderException) exception).getErrorModel())
                .isEqualTo(PurchaseOrderErrorCode.PURCHASE_ORDER_ACCESS_DENIED);

        PurchaseOrderDetailResponse confirmed = purchaseOrderService.confirm(userId, order.getPurchaseOrderId());
        assertThat(confirmed.status()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        assertThat(confirmed.confirmedByUserId()).isEqualTo(userId);
    }

    private Store createStore(Long storeId) {
        Store store = Store.create("store", "1234567890", "address", "01012345678");
        ReflectionTestUtils.setField(store, "storeId", storeId);
        return store;
    }

    private PurchaseOrderDetailResponse toDetailResponse(PurchaseOrder purchaseOrder) {
        return new PurchaseOrderDetailResponse(
                purchaseOrder.getPurchaseOrderId(),
                purchaseOrder.getStore().getStoreId(),
                purchaseOrder.getOrderNo(),
                purchaseOrder.getStatus(),
                purchaseOrder.getTotalAmount(),
                purchaseOrder.getSubmittedByUserId(),
                purchaseOrder.getSubmittedAt(),
                purchaseOrder.getConfirmedByUserId(),
                purchaseOrder.getConfirmedAt(),
                purchaseOrder.getCanceledByUserId(),
                purchaseOrder.getCanceledAt(),
                purchaseOrder.getItems().stream()
                        .map(item -> new PurchaseOrderItemResponse(
                                item.getItemName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getLineAmount()
                        ))
                        .toList()
        );
    }
}

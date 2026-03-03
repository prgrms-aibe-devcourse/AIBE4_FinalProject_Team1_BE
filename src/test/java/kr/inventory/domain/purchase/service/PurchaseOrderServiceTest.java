package kr.inventory.domain.purchase.service;

import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderItemRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderUpdateRequest;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderSummaryResponse;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.PurchaseOrderItem;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.purchase.exception.PurchaseOrderErrorCode;
import kr.inventory.domain.purchase.exception.PurchaseOrderException;
import kr.inventory.domain.purchase.repository.PurchaseOrderItemRepository;
import kr.inventory.domain.purchase.repository.PurchaseOrderRepository;
import kr.inventory.domain.purchase.validator.PurchaseOrderValidator;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("발주서 서비스 테스트")
class PurchaseOrderServiceTest {

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private PurchaseOrderPdfService purchaseOrderPdfService;

    @Mock
    private PurchaseOrderValidator purchaseOrderValidator;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private StoreAccessValidator storeAccessValidator;

    private Store store;
    private Vendor vendor;
    private PurchaseOrder purchaseOrder;
    private UUID storePublicId;
    private UUID vendorPublicId;
    private UUID purchaseOrderPublicId;

    @BeforeEach
    void setUp() {
        // Store 생성
        store = Store.create("테스트 매장", "1234567890");
        ReflectionTestUtils.setField(store, "storeId", 1L);
        storePublicId = store.getStorePublicId();

        // Vendor 생성
        vendor = Vendor.create(store, "테스트 거래처", "김철수", "010-1234-5678", "test@vendor.com", 2);
        ReflectionTestUtils.setField(vendor, "vendorId", 1L);
        vendorPublicId = vendor.getVendorPublicId();

        // PurchaseOrder 생성
        purchaseOrder = PurchaseOrder.createDraft(store);
        purchaseOrder.assignVendor(vendor);
        ReflectionTestUtils.setField(purchaseOrder, "purchaseOrderId", 1L);
        purchaseOrderPublicId = purchaseOrder.getPurchaseOrderPublicId();
    }

    @Test
    @DisplayName("발주서 초안 생성 성공")
    void givenValidRequest_whenCreateDraft_thenSuccess() {
        // given
        Long userId = 1L;

        PurchaseOrderItemRequest item1 = new PurchaseOrderItemRequest("양파", 10, new BigDecimal("1000"));
        PurchaseOrderItemRequest item2 = new PurchaseOrderItemRequest("감자", 20, new BigDecimal("800"));
        PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest(vendorPublicId, List.of(item1, item2));

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        given(storeRepository.findById(1L))
                .willReturn(Optional.of(store));

        given(vendorRepository.findByVendorPublicId(vendorPublicId))
                .willReturn(Optional.of(vendor));

        given(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .willReturn(purchaseOrder);

        List<PurchaseOrderItem> savedItems = List.of(
                PurchaseOrderItem.create("양파", 10, new BigDecimal("1000")),
                PurchaseOrderItem.create("감자", 20, new BigDecimal("800"))
        );
        given(purchaseOrderItemRepository.saveAll(anyList()))
                .willReturn(savedItems);

        // when
        PurchaseOrderDetailResponse response = purchaseOrderService.createDraft(userId, storePublicId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.purchaseOrderPublicId()).isEqualTo(purchaseOrderPublicId);
        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.DRAFT);

        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
        verify(purchaseOrderItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("발주서 초안 생성 실패 - 매장을 찾을 수 없음")
    void givenNonExistentStore_whenCreateDraft_thenThrowException() {
        // given
        Long userId = 1L;
        PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest(vendorPublicId, Collections.emptyList());

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        given(storeRepository.findById(1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> purchaseOrderService.createDraft(userId, storePublicId, request))
                .isInstanceOf(PurchaseOrderException.class)
                .hasMessageContaining(PurchaseOrderErrorCode.STORE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("발주서 초안 생성 실패 - 거래처를 찾을 수 없음")
    void givenNonExistentVendor_whenCreateDraft_thenThrowException() {
        // given
        Long userId = 1L;
        UUID invalidVendorId = UUID.randomUUID();
        PurchaseOrderItemRequest item = new PurchaseOrderItemRequest("양파", 10, new BigDecimal("1000"));
        PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest(invalidVendorId, List.of(item));

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        given(storeRepository.findById(1L))
                .willReturn(Optional.of(store));

        given(vendorRepository.findByVendorPublicId(invalidVendorId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> purchaseOrderService.createDraft(userId, storePublicId, request))
                .isInstanceOf(PurchaseOrderException.class)
                .hasMessageContaining(PurchaseOrderErrorCode.VENDOR_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("발주서 초안 생성 실패 - 비활성화된 거래처")
    void givenInactiveVendor_whenCreateDraft_thenThrowException() {
        // given
        Long userId = 1L;
        PurchaseOrderItemRequest item = new PurchaseOrderItemRequest("양파", 10, new BigDecimal("1000"));
        PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest(vendorPublicId, List.of(item));

        // 거래처 비활성화
        vendor.deactivate();

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        given(storeRepository.findById(1L))
                .willReturn(Optional.of(store));

        given(vendorRepository.findByVendorPublicId(vendorPublicId))
                .willReturn(Optional.of(vendor));

        // when & then
        assertThatThrownBy(() -> purchaseOrderService.createDraft(userId, storePublicId, request))
                .isInstanceOf(PurchaseOrderException.class)
                .hasMessageContaining(PurchaseOrderErrorCode.VENDOR_NOT_ACTIVE.getMessage());
    }

    @Test
    @DisplayName("발주서 목록 조회 성공")
    void givenValidUser_whenGetPurchaseOrders_thenSuccess() {
        // given
        Long userId = 1L;

        PurchaseOrder order1 = PurchaseOrder.createDraft(store);
        ReflectionTestUtils.setField(order1, "purchaseOrderId", 1L);
        PurchaseOrder order2 = PurchaseOrder.createDraft(store);
        ReflectionTestUtils.setField(order2, "purchaseOrderId", 2L);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        given(purchaseOrderRepository.findAllByStoreStoreIdOrderByPurchaseOrderIdDesc(1L))
                .willReturn(List.of(order2, order1));

        // when
        List<PurchaseOrderSummaryResponse> responses = purchaseOrderService.getPurchaseOrders(userId, storePublicId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).purchaseOrderPublicId()).isEqualTo(order2.getPurchaseOrderPublicId());
        assertThat(responses.get(1).purchaseOrderPublicId()).isEqualTo(order1.getPurchaseOrderPublicId());
    }

    @Test
    @DisplayName("발주서 상세 조회 성공")
    void givenValidPurchaseOrder_whenGetPurchaseOrder_thenSuccess() {
        // given
        Long userId = 1L;

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        given(purchaseOrderValidator.validateAccessAndGetPurchaseOrderId(userId, purchaseOrderPublicId))
                .willReturn(1L);

        given(purchaseOrderRepository.findById(1L))
                .willReturn(Optional.of(purchaseOrder));

        List<PurchaseOrderItem> items = List.of(
                PurchaseOrderItem.create("양파", 10, new BigDecimal("1000"))
        );
        given(purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(1L))
                .willReturn(items);

        // when
        PurchaseOrderDetailResponse response = purchaseOrderService.getPurchaseOrder(userId, storePublicId, purchaseOrderPublicId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.purchaseOrderPublicId()).isEqualTo(purchaseOrderPublicId);
        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("발주서 초안 수정 성공")
    void givenDraftOrder_whenUpdateDraft_thenSuccess() {
        // given
        Long userId = 1L;

        PurchaseOrderItemRequest item1 = new PurchaseOrderItemRequest("당근", 15, new BigDecimal("1200"));
        PurchaseOrderUpdateRequest request = new PurchaseOrderUpdateRequest(vendorPublicId, List.of(item1));

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        given(purchaseOrderValidator.validateAccessAndGetPurchaseOrderId(userId, purchaseOrderPublicId))
                .willReturn(1L);

        given(purchaseOrderRepository.findById(1L))
                .willReturn(Optional.of(purchaseOrder));

        given(vendorRepository.findByVendorPublicId(vendorPublicId))
                .willReturn(Optional.of(vendor));

        List<PurchaseOrderItem> oldItems = List.of(
                PurchaseOrderItem.create("양파", 10, new BigDecimal("1000"))
        );
        given(purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(1L))
                .willReturn(oldItems);

        List<PurchaseOrderItem> newItems = List.of(
                PurchaseOrderItem.create("당근", 15, new BigDecimal("1200"))
        );
        given(purchaseOrderItemRepository.saveAll(anyList()))
                .willReturn(newItems);

        // when
        PurchaseOrderDetailResponse response = purchaseOrderService.updateDraft(userId, storePublicId, purchaseOrderPublicId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);

        verify(purchaseOrderItemRepository).deleteAll(oldItems);
        verify(purchaseOrderItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("발주서 제출 성공")
    void givenDraftOrder_whenSubmit_thenSuccess() {
        // given
        Long userId = 1L;

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        given(purchaseOrderValidator.validateAccessAndGetPurchaseOrderId(userId, purchaseOrderPublicId))
                .willReturn(1L);

        given(purchaseOrderRepository.findById(1L))
                .willReturn(Optional.of(purchaseOrder));

        given(purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(1L))
                .willReturn(Collections.emptyList());

        // when
        PurchaseOrderDetailResponse response = purchaseOrderService.submit(userId, storePublicId, purchaseOrderPublicId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.SUBMITTED);
        assertThat(response.orderNo()).isNotNull();
        assertThat(response.submittedAt()).isNotNull();
    }

    @Test
    @DisplayName("발주서 확정 성공")
    void givenSubmittedOrder_whenConfirm_thenSuccess() {
        // given
        Long userId = 1L;

        // 발주서를 SUBMITTED 상태로 설정
        purchaseOrder.submit("PO-20250303-001", userId, java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        given(purchaseOrderValidator.validateAccessAndGetPurchaseOrderId(userId, purchaseOrderPublicId))
                .willReturn(1L);

        given(purchaseOrderRepository.findById(1L))
                .willReturn(Optional.of(purchaseOrder));

        given(purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(1L))
                .willReturn(Collections.emptyList());

        // when
        PurchaseOrderDetailResponse response = purchaseOrderService.confirm(userId, storePublicId, purchaseOrderPublicId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        assertThat(response.confirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("발주서 취소 성공")
    void givenCancelableOrder_whenCancel_thenSuccess() {
        // given
        Long userId = 1L;

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        given(purchaseOrderValidator.validateAccessAndGetPurchaseOrderId(userId, purchaseOrderPublicId))
                .willReturn(1L);

        given(purchaseOrderRepository.findById(1L))
                .willReturn(Optional.of(purchaseOrder));

        given(purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(1L))
                .willReturn(Collections.emptyList());

        // when
        PurchaseOrderDetailResponse response = purchaseOrderService.cancel(userId, storePublicId, purchaseOrderPublicId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.CANCELED);
        assertThat(response.canceledAt()).isNotNull();
    }

    @Test
    @DisplayName("PDF 다운로드 성공")
    void givenValidPurchaseOrder_whenDownloadPdf_thenSuccess() {
        // given
        Long userId = 1L;
        byte[] expectedPdfBytes = "PDF_CONTENT".getBytes();

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        given(purchaseOrderValidator.validateAccessAndGetPurchaseOrderId(userId, purchaseOrderPublicId))
                .willReturn(1L);

        given(purchaseOrderRepository.findById(1L))
                .willReturn(Optional.of(purchaseOrder));

        given(purchaseOrderPdfService.generate(purchaseOrder))
                .willReturn(expectedPdfBytes);

        // when
        byte[] pdfBytes = purchaseOrderService.downloadPdf(userId, storePublicId, purchaseOrderPublicId);

        // then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes).isEqualTo(expectedPdfBytes);

        verify(purchaseOrderPdfService).generate(purchaseOrder);
    }
}
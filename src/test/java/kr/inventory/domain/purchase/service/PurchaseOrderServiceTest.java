package kr.inventory.domain.purchase.service;

import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderItemRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderUpdateRequest;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderSummaryResponse;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.PurchaseOrderItem;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.purchase.repository.PurchaseOrderItemRepository;
import kr.inventory.domain.purchase.repository.PurchaseOrderRepository;
import kr.inventory.domain.purchase.validator.PurchaseOrderValidator;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.reference.entity.Vendor;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderSearchRequest;
import kr.inventory.domain.reference.repository.VendorRepository;
import kr.inventory.global.common.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

        @Mock
        private kr.inventory.domain.user.repository.UserRepository userRepository;

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
                purchaseOrder = PurchaseOrder.create(store);
                purchaseOrder.assignVendor(vendor);
                ReflectionTestUtils.setField(purchaseOrder, "purchaseOrderId", 1L);
                purchaseOrderPublicId = purchaseOrder.getPurchaseOrderPublicId();
        }

        @Test
        @DisplayName("발주서 생성 성공")
        void givenValidRequest_whenCreate_thenSuccess() {
                // given
                Long userId = 1L;

                PurchaseOrderItemRequest item1 = new PurchaseOrderItemRequest("양파", 10, "EA", new BigDecimal("1000"));
                PurchaseOrderItemRequest item2 = new PurchaseOrderItemRequest("감자", 20, "KG", new BigDecimal("800"));
                PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest(vendorPublicId,
                                List.of(item1, item2));

                given(storeAccessValidator.validateAndGetStoreIdForActiveMembers(userId, storePublicId))
                                .willReturn(1L);

                given(storeRepository.findById(1L))
                                .willReturn(Optional.of(store));

                given(vendorRepository.findByVendorPublicId(vendorPublicId))
                                .willReturn(Optional.of(vendor));

                given(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                                .willReturn(purchaseOrder);

                List<PurchaseOrderItem> savedItems = List.of(
                                PurchaseOrderItem.create("양파", 10, "EA", new BigDecimal("1000")),
                                PurchaseOrderItem.create("감자", 20, "KG", new BigDecimal("800")));
                given(purchaseOrderItemRepository.saveAll(anyList()))
                                .willReturn(savedItems);

                // when
                PurchaseOrderDetailResponse response = purchaseOrderService.create(userId, storePublicId, request);

                // then
                assertThat(response).isNotNull();
                assertThat(response.purchaseOrderPublicId()).isEqualTo(purchaseOrderPublicId);
                assertThat(response.status()).isEqualTo(PurchaseOrderStatus.ORDERED);
                assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("26000"));

                verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
                verify(purchaseOrderItemRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("발주서 목록 조회 성공")
        void givenValidUser_whenGetPurchaseOrders_thenSuccess() {
                // given
                Long userId = 1L;
                PurchaseOrderSearchRequest searchRequest = new PurchaseOrderSearchRequest(null, null);
                Pageable pageable = PageRequest.of(0, 10);

                PurchaseOrder order1 = PurchaseOrder.create(store);
                ReflectionTestUtils.setField(order1, "purchaseOrderId", 1L);
                PurchaseOrder order2 = PurchaseOrder.create(store);
                ReflectionTestUtils.setField(order2, "purchaseOrderId", 2L);

                given(storeAccessValidator.validateAndGetStoreIdForActiveMembers(userId, storePublicId))
                                .willReturn(1L);

                Page<PurchaseOrder> page = new PageImpl<>(List.of(order2, order1), pageable, 2);
                given(purchaseOrderRepository.findByStoreIdWithFilters(eq(1L), eq(searchRequest), any(Pageable.class)))
                                .willReturn(page);

                // when
                PageResponse<PurchaseOrderSummaryResponse> response = purchaseOrderService.getPurchaseOrders(userId,
                                storePublicId, searchRequest, pageable);

                // then
                assertThat(response.content()).hasSize(2);
                assertThat(response.content().get(0).purchaseOrderPublicId())
                                .isEqualTo(order2.getPurchaseOrderPublicId());
                assertThat(response.content().get(1).purchaseOrderPublicId())
                                .isEqualTo(order1.getPurchaseOrderPublicId());
                assertThat(response.totalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("발주서 상세 조회 성공")
        void givenValidPurchaseOrder_whenGetPurchaseOrder_thenSuccess() {
                // given
                Long userId = 1L;

                given(storeAccessValidator.validateAndGetStoreIdForActiveMembers(userId, storePublicId))
                                .willReturn(1L);

                given(purchaseOrderValidator.validateAccessAndGetPurchaseOrderId(userId, purchaseOrderPublicId))
                                .willReturn(1L);

                given(purchaseOrderRepository.findById(1L))
                                .willReturn(Optional.of(purchaseOrder));

                List<PurchaseOrderItem> items = List.of(
                                PurchaseOrderItem.create("양파", 10, "EA", new BigDecimal("1000")));
                given(purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(1L))
                                .willReturn(items);

                // when
                PurchaseOrderDetailResponse response = purchaseOrderService.getPurchaseOrder(userId, storePublicId,
                                purchaseOrderPublicId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.purchaseOrderPublicId()).isEqualTo(purchaseOrderPublicId);
                assertThat(response.items()).hasSize(1);
        }

        @Test
        @DisplayName("발주서 수정 성공")
        void givenOrder_whenUpdate_thenSuccess() {
                // given
                Long userId = 1L;

                PurchaseOrderItemRequest item1 = new PurchaseOrderItemRequest("당근", 15, "KG", new BigDecimal("1200"));
                PurchaseOrderUpdateRequest request = new PurchaseOrderUpdateRequest(vendorPublicId, List.of(item1));

                given(storeAccessValidator.validateAndGetStoreIdForActiveMembers(userId, storePublicId))
                                .willReturn(1L);

                given(purchaseOrderValidator.validateAccessAndGetPurchaseOrderId(userId, purchaseOrderPublicId))
                                .willReturn(1L);

                given(purchaseOrderRepository.findById(1L))
                                .willReturn(Optional.of(purchaseOrder));

                given(vendorRepository.findByVendorPublicId(vendorPublicId))
                                .willReturn(Optional.of(vendor));

                List<PurchaseOrderItem> oldItems = List.of(
                                PurchaseOrderItem.create("양파", 10, "EA", new BigDecimal("1000")));
                given(purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(1L))
                                .willReturn(oldItems);

                List<PurchaseOrderItem> newItems = List.of(
                                PurchaseOrderItem.create("당근", 15, "KG", new BigDecimal("1200")));
                given(purchaseOrderItemRepository.saveAll(anyList()))
                                .willReturn(newItems);

                // when
                PurchaseOrderDetailResponse response = purchaseOrderService.update(userId, storePublicId,
                                purchaseOrderPublicId, request);

                // then
                assertThat(response).isNotNull();
                assertThat(response.items()).hasSize(1);
                assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("18000"));

                verify(purchaseOrderItemRepository).deleteAllInBatch(oldItems);
                verify(purchaseOrderItemRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("발주서 취소 성공")
        void givenCancelableOrder_whenCancel_thenSuccess() {
                // given
                Long userId = 1L;

                given(storeAccessValidator.validateAndGetStoreIdForActiveMembers(userId, storePublicId))
                                .willReturn(1L);

                given(purchaseOrderValidator.validateAccessAndGetPurchaseOrderId(userId, purchaseOrderPublicId))
                                .willReturn(1L);

                given(purchaseOrderRepository.findById(1L))
                                .willReturn(Optional.of(purchaseOrder));

                given(purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(1L))
                                .willReturn(Collections.emptyList());

                // when
                PurchaseOrderDetailResponse response = purchaseOrderService.cancel(userId, storePublicId,
                                purchaseOrderPublicId);

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

                given(storeAccessValidator.validateAndGetStoreIdForActiveMembers(userId, storePublicId))
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
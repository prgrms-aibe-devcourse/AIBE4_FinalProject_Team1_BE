package kr.inventory.domain.reference.service;

import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.reference.controller.dto.request.VendorCreateRequest;
import kr.inventory.domain.reference.controller.dto.request.VendorSearchRequest;
import kr.inventory.domain.reference.controller.dto.response.VendorResponse;
import kr.inventory.domain.reference.controller.dto.request.VendorUpdateRequest;
import kr.inventory.domain.reference.entity.Vendor;
import kr.inventory.domain.reference.entity.enums.VendorStatus;
import kr.inventory.domain.reference.exception.VendorErrorCode;
import kr.inventory.domain.reference.exception.VendorException;
import kr.inventory.domain.reference.repository.VendorRepository;
import kr.inventory.global.dto.PageResponse;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("VendorService 단위 테스트")
class VendorServiceTest {

        @Mock
        private VendorRepository vendorRepository;

        @Mock
        private StoreRepository storeRepository;

        @Mock
        private StoreAccessValidator storeAccessValidator;

        @InjectMocks
        private VendorService vendorService;

        @Test
        @DisplayName("거래처 등록 성공")
        void givenValidRequest_whenCreateVendor_thenSuccess() {
                // given
                Long userId = 1L;
                UUID storePublicId = UUID.randomUUID();
                Long storeId = 1L;
                Store store = Store.create("청춘식당", "1234567890");
                ReflectionTestUtils.setField(store, "storeId", storeId);

                VendorCreateRequest request = new VendorCreateRequest(
                                "신선마트",
                                "김철수",
                                "010-1234-5678",
                                "fresh@market.com",
                                2);

                Vendor vendor = Vendor.create(
                                store,
                                request.name(),
                                request.contactPerson(),
                                request.phone(),
                                request.email(),
                                request.leadTimeDays());

                given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
                given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
                given(vendorRepository.existsByStoreStoreIdAndName(storeId, request.name())).willReturn(false);
                given(vendorRepository.save(any(Vendor.class))).willReturn(vendor);

                // when
                VendorResponse response = vendorService.createVendor(userId, storePublicId, request);

                // then
                assertThat(response).isNotNull();
                assertThat(response.name()).isEqualTo("신선마트");
                assertThat(response.contactPerson()).isEqualTo("김철수");
                assertThat(response.phone()).isEqualTo("010-1234-5678");
                assertThat(response.email()).isEqualTo("fresh@market.com");
                assertThat(response.leadTimeDays()).isEqualTo(2);
                assertThat(response.status()).isEqualTo(VendorStatus.ACTIVE);

                verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
                verify(storeRepository).findById(storeId);
                verify(vendorRepository).existsByStoreStoreIdAndName(storeId, request.name());
                verify(vendorRepository).save(any(Vendor.class));
        }

        @Test
        @DisplayName("거래처 등록 실패 - 매장 없음")
        void givenInvalidStoreId_whenCreateVendor_thenThrowException() {
                // given
                Long userId = 1L;
                UUID storePublicId = UUID.randomUUID();
                Long storeId = 999L;
                VendorCreateRequest request = new VendorCreateRequest(
                                "신선마트",
                                "김철수",
                                "010-1234-5678",
                                "fresh@market.com",
                                2);

                given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
                given(storeRepository.findById(storeId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> vendorService.createVendor(userId, storePublicId, request))
                                .isInstanceOf(StoreException.class)
                                .extracting("errorModel")
                                .isEqualTo(StoreErrorCode.STORE_NOT_FOUND);

                verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
                verify(storeRepository).findById(storeId);
        }

        @Test
        @DisplayName("거래처 등록 실패 - 거래처명 중복")
        void givenDuplicateName_whenCreateVendor_thenThrowException() {
                // given
                Long userId = 1L;
                UUID storePublicId = UUID.randomUUID();
                Long storeId = 1L;
                Store store = Store.create("청춘식당", "1234567890");

                VendorCreateRequest request = new VendorCreateRequest(
                                "신선마트",
                                "김철수",
                                "010-1234-5678",
                                "fresh@market.com",
                                2);

                given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
                given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
                given(vendorRepository.existsByStoreStoreIdAndName(storeId, request.name())).willReturn(true);

                // when & then
                assertThatThrownBy(() -> vendorService.createVendor(userId, storePublicId, request))
                                .isInstanceOf(VendorException.class)
                                .extracting("errorModel")
                                .isEqualTo(VendorErrorCode.VENDOR_DUPLICATE_NAME);

                verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
                verify(storeRepository).findById(storeId);
                verify(vendorRepository).existsByStoreStoreIdAndName(storeId, request.name());
        }

        @Test
        @DisplayName("매장별 거래처 목록 조회 성공")
        void givenStorePublicId_whenGetVendorsByStore_thenReturnList() {
                // given
                Long userId = 1L;
                UUID storePublicId = UUID.randomUUID();
                Long storeId = 1L;
                Store store = Store.create("청춘식당", "1234567890");

                Vendor vendor1 = Vendor.create(store, "신선마트", "김철수", "010-1111-1111", "v1@test.com", 1);
                Vendor vendor2 = Vendor.create(store, "농협마트", "이영희", "010-2222-2222", "v2@test.com", 2);

                VendorSearchRequest searchRequest = new VendorSearchRequest(VendorStatus.ACTIVE, null);
                Pageable pageable = PageRequest.of(0, 10);
                Page<Vendor> vendorPage = new PageImpl<>(List.of(vendor1, vendor2), pageable, 2);

                given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
                given(vendorRepository.findByStoreIdWithFilters(eq(storeId), eq(VendorStatus.ACTIVE), any(),
                                eq(pageable)))
                                .willReturn(vendorPage);

                // when
                PageResponse<VendorResponse> responses = vendorService.getVendorsByStore(userId, storePublicId,
                                searchRequest, pageable);

                // then
                assertThat(responses.content()).hasSize(2);
                assertThat(responses.content().get(0).name()).isEqualTo("신선마트");
                assertThat(responses.content().get(1).name()).isEqualTo("농협마트");

                verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
                verify(vendorRepository).findByStoreIdWithFilters(eq(storeId), eq(VendorStatus.ACTIVE), any(),
                                eq(pageable));
        }

        @Test
        @DisplayName("거래처 상세 조회 성공")
        void givenVendorPublicId_whenGetVendor_thenReturnVendor() {
                // given
                Long userId = 1L;
                UUID storePublicId = UUID.randomUUID();
                UUID vendorPublicId = UUID.randomUUID();
                Long storeId = 1L;

                Store store = Store.create("청춘식당", "1234567890");
                ReflectionTestUtils.setField(store, "storeId", storeId);

                Vendor vendor = Vendor.create(store, "신선마트", "김철수", "010-1234-5678", "fresh@market.com", 2);

                given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
                given(vendorRepository.findByVendorPublicId(vendorPublicId)).willReturn(Optional.of(vendor));

                // when
                VendorResponse response = vendorService.getVendor(storePublicId, vendorPublicId, userId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.name()).isEqualTo("신선마트");
                assertThat(response.contactPerson()).isEqualTo("김철수");

                verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
                verify(vendorRepository).findByVendorPublicId(vendorPublicId);
        }

        @Test
        @DisplayName("거래처 상세 조회 실패 - 거래처 없음")
        void givenInvalidVendorPublicId_whenGetVendor_thenThrowException() {
                // given
                Long userId = 1L;
                UUID storePublicId = UUID.randomUUID();
                UUID vendorPublicId = UUID.randomUUID();
                Long storeId = 1L;

                given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
                given(vendorRepository.findByVendorPublicId(vendorPublicId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> vendorService.getVendor(storePublicId, vendorPublicId, userId))
                                .isInstanceOf(VendorException.class)
                                .extracting("errorModel")
                                .isEqualTo(VendorErrorCode.VENDOR_NOT_FOUND);

                verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
                verify(vendorRepository).findByVendorPublicId(vendorPublicId);
        }

        @Test
        @DisplayName("거래처 수정 성공")
        void givenValidRequest_whenUpdateVendor_thenSuccess() {
                // given
                Long userId = 1L;
                UUID storePublicId = UUID.randomUUID();
                UUID vendorPublicId = UUID.randomUUID();
                Long storeId = 1L;

                Store store = Store.create("청춘식당", "1234567890");
                ReflectionTestUtils.setField(store, "storeId", storeId);

                Vendor vendor = Vendor.create(store, "신선마트", "김철수", "010-1234-5678", "fresh@market.com", 2);

                VendorUpdateRequest request = new VendorUpdateRequest(
                                "박영수",
                                "010-9999-9999",
                                "updated@market.com",
                                3,
                                VendorStatus.ACTIVE);

                given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
                given(vendorRepository.findByVendorPublicId(vendorPublicId)).willReturn(Optional.of(vendor));

                // when
                VendorResponse response = vendorService.updateVendor(storePublicId, vendorPublicId, userId, request);

                // then
                assertThat(response).isNotNull();
                assertThat(response.contactPerson()).isEqualTo("박영수");
                assertThat(response.phone()).isEqualTo("010-9999-9999");
                assertThat(response.email()).isEqualTo("updated@market.com");
                assertThat(response.leadTimeDays()).isEqualTo(3);

                verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
                verify(vendorRepository).findByVendorPublicId(vendorPublicId);
        }

        @Test
        @DisplayName("거래처 수정 실패 - 거래처 없음")
        void givenInvalidVendorPublicId_whenUpdateVendor_thenThrowException() {
                // given
                Long userId = 1L;
                UUID storePublicId = UUID.randomUUID();
                UUID vendorPublicId = UUID.randomUUID();
                Long storeId = 1L;

                VendorUpdateRequest request = new VendorUpdateRequest(
                                "박영수",
                                "010-9999-9999",
                                "updated@market.com",
                                3,
                                VendorStatus.ACTIVE);

                given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
                given(vendorRepository.findByVendorPublicId(vendorPublicId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> vendorService.updateVendor(storePublicId, vendorPublicId, userId, request))
                                .isInstanceOf(VendorException.class)
                                .extracting("errorModel")
                                .isEqualTo(VendorErrorCode.VENDOR_NOT_FOUND);

                verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
                verify(vendorRepository).findByVendorPublicId(vendorPublicId);
        }

        @Test
        @DisplayName("거래처 비활성화 성공")
        void givenVendorPublicId_whenDeactivateVendor_thenSuccess() {
                // given
                Long userId = 1L;
                UUID storePublicId = UUID.randomUUID();
                UUID vendorPublicId = UUID.randomUUID();
                Long storeId = 1L;

                Store store = Store.create("청춘식당", "1234567890");
                ReflectionTestUtils.setField(store, "storeId", storeId);

                Vendor vendor = Vendor.create(store, "신선마트", "김철수", "010-1234-5678", "fresh@market.com", 2);

                given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
                given(vendorRepository.findByVendorPublicId(vendorPublicId)).willReturn(Optional.of(vendor));

                // when
                vendorService.deactivateVendor(storePublicId, vendorPublicId, userId);

                // then
                assertThat(vendor.getStatus()).isEqualTo(VendorStatus.INACTIVE);

                verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
                verify(vendorRepository).findByVendorPublicId(vendorPublicId);
        }

        @Test
        @DisplayName("거래처 비활성화 실패 - 거래처 없음")
        void givenInvalidVendorPublicId_whenDeactivateVendor_thenThrowException() {
                // given
                Long userId = 1L;
                UUID storePublicId = UUID.randomUUID();
                UUID vendorPublicId = UUID.randomUUID();
                Long storeId = 1L;

                given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
                given(vendorRepository.findByVendorPublicId(vendorPublicId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> vendorService.deactivateVendor(storePublicId, vendorPublicId, userId))
                                .isInstanceOf(VendorException.class)
                                .extracting("errorModel")
                                .isEqualTo(VendorErrorCode.VENDOR_NOT_FOUND);

                verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
                verify(vendorRepository).findByVendorPublicId(vendorPublicId);
        }
}
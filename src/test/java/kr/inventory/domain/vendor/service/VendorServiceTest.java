package kr.inventory.domain.vendor.service;

import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.vendor.controller.dto.VendorCreateRequest;
import kr.inventory.domain.vendor.controller.dto.VendorResponse;
import kr.inventory.domain.vendor.controller.dto.VendorUpdateRequest;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.exception.VendorErrorCode;
import kr.inventory.domain.vendor.exception.VendorException;
import kr.inventory.domain.vendor.repository.VendorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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

    @InjectMocks
    private VendorService vendorService;

    @Test
    @DisplayName("거래처 등록 성공")
    void givenValidRequest_whenCreateVendor_thenSuccess() {
        // given
        Long storeId = 1L;
        Store store = Store.create("청춘식당", "1234567890");

        VendorCreateRequest request = new VendorCreateRequest(
                "신선마트",
                "김철수",
                "010-1234-5678",
                "fresh@market.com",
                2
        );

        Vendor vendor = Vendor.create(
                store,
                request.name(),
                request.contactPerson(),
                request.phone(),
                request.email(),
                request.leadTimeDays()
        );

        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(vendorRepository.existsByStoreStoreIdAndName(storeId, request.name())).willReturn(false);
        given(vendorRepository.save(any(Vendor.class))).willReturn(vendor);

        // when
        VendorResponse response = vendorService.createVendor(storeId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("신선마트");
        assertThat(response.contactPerson()).isEqualTo("김철수");
        assertThat(response.phone()).isEqualTo("010-1234-5678");
        assertThat(response.email()).isEqualTo("fresh@market.com");
        assertThat(response.leadTimeDays()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(VendorStatus.ACTIVE);

        verify(storeRepository).findById(storeId);
        verify(vendorRepository).existsByStoreStoreIdAndName(storeId, request.name());
        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    @DisplayName("거래처 등록 실패 - 매장 없음")
    void givenInvalidStoreId_whenCreateVendor_thenThrowException() {
        // given
        Long invalidStoreId = 999L;
        VendorCreateRequest request = new VendorCreateRequest(
                "신선마트",
                "김철수",
                "010-1234-5678",
                "fresh@market.com",
                2
        );

        given(storeRepository.findById(invalidStoreId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> vendorService.createVendor(invalidStoreId, request))
                .isInstanceOf(StoreException.class)
                .extracting("errorModel")
                .isEqualTo(StoreErrorCode.STORE_NOT_FOUND_OR_ACCESS_DENIED);

        verify(storeRepository).findById(invalidStoreId);
    }

    @Test
    @DisplayName("거래처 등록 실패 - 거래처명 중복")
    void givenDuplicateName_whenCreateVendor_thenThrowException() {
        // given
        Long storeId = 1L;
        Store store = Store.create("청춘식당", "1234567890");

        VendorCreateRequest request = new VendorCreateRequest(
                "신선마트",
                "김철수",
                "010-1234-5678",
                "fresh@market.com",
                2
        );

        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(vendorRepository.existsByStoreStoreIdAndName(storeId, request.name())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> vendorService.createVendor(storeId, request))
                .isInstanceOf(VendorException.class)
                .extracting("errorModel")
                .isEqualTo(VendorErrorCode.VENDOR_DUPLICATE_NAME);

        verify(storeRepository).findById(storeId);
        verify(vendorRepository).existsByStoreStoreIdAndName(storeId, request.name());
    }

    @Test
    @DisplayName("매장별 거래처 목록 조회 성공")
    void givenStoreId_whenGetVendorsByStore_thenReturnList() {
        // given
        Long storeId = 1L;
        Store store = Store.create("청춘식당", "1234567890");

        Vendor vendor1 = Vendor.create(store, "신선마트", "김철수", "010-1111-1111", "v1@test.com", 1);
        Vendor vendor2 = Vendor.create(store, "농협마트", "이영희", "010-2222-2222", "v2@test.com", 2);

        given(vendorRepository.findByStoreIdWithFilters(storeId, null))
                .willReturn(List.of(vendor1, vendor2));

        // when
        List<VendorResponse> responses = vendorService.getVendorsByStore(storeId, null);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("신선마트");
        assertThat(responses.get(1).name()).isEqualTo("농협마트");

        verify(vendorRepository).findByStoreIdWithFilters(storeId, null);
    }

    @Test
    @DisplayName("매장별 거래처 목록 조회 - ACTIVE만 필터링")
    void givenStoreIdAndActiveStatus_whenGetVendorsByStore_thenReturnActiveOnly() {
        // given
        Long storeId = 1L;
        Store store = Store.create("청춘식당", "1234567890");

        Vendor activeVendor = Vendor.create(store, "신선마트", "김철수", "010-1111-1111", "v1@test.com", 1);

        given(vendorRepository.findByStoreIdWithFilters(storeId, VendorStatus.ACTIVE))
                .willReturn(List.of(activeVendor));

        // when
        List<VendorResponse> responses = vendorService.getVendorsByStore(storeId, VendorStatus.ACTIVE);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(VendorStatus.ACTIVE);

        verify(vendorRepository).findByStoreIdWithFilters(storeId, VendorStatus.ACTIVE);
    }

    @Test
    @DisplayName("거래처 상세 조회 성공")
    void givenVendorId_whenGetVendor_thenReturnVendor() {
        // given
        Long vendorId = 1L;
        Store store = Store.create("청춘식당", "1234567890");
        Vendor vendor = Vendor.create(store, "신선마트", "김철수", "010-1234-5678", "fresh@market.com", 2);

        given(vendorRepository.findById(vendorId)).willReturn(Optional.of(vendor));

        // when
        VendorResponse response = vendorService.getVendor(vendorId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("신선마트");
        assertThat(response.contactPerson()).isEqualTo("김철수");

        verify(vendorRepository).findById(vendorId);
    }

    @Test
    @DisplayName("거래처 상세 조회 실패 - 거래처 없음")
    void givenInvalidVendorId_whenGetVendor_thenThrowException() {
        // given
        Long invalidVendorId = 999L;

        given(vendorRepository.findById(invalidVendorId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> vendorService.getVendor(invalidVendorId))
                .isInstanceOf(VendorException.class)
                .extracting("errorModel")
                .isEqualTo(VendorErrorCode.VENDOR_NOT_FOUND);

        verify(vendorRepository).findById(invalidVendorId);
    }

    @Test
    @DisplayName("거래처 수정 성공")
    void givenValidRequest_whenUpdateVendor_thenSuccess() {
        // given
        Long vendorId = 1L;
        Store store = Store.create("청춘식당", "1234567890");
        Vendor vendor = Vendor.create(store, "신선마트", "김철수", "010-1234-5678", "fresh@market.com", 2);

        VendorUpdateRequest request = new VendorUpdateRequest(
                "박영수",
                "010-9999-9999",
                "updated@market.com",
                3
        );

        given(vendorRepository.findById(vendorId)).willReturn(Optional.of(vendor));

        // when
        VendorResponse response = vendorService.updateVendor(vendorId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.contactPerson()).isEqualTo("박영수");
        assertThat(response.phone()).isEqualTo("010-9999-9999");
        assertThat(response.email()).isEqualTo("updated@market.com");
        assertThat(response.leadTimeDays()).isEqualTo(3);

        verify(vendorRepository).findById(vendorId);
    }

    @Test
    @DisplayName("거래처 수정 실패 - 거래처 없음")
    void givenInvalidVendorId_whenUpdateVendor_thenThrowException() {
        // given
        Long invalidVendorId = 999L;
        VendorUpdateRequest request = new VendorUpdateRequest(
                "박영수",
                "010-9999-9999",
                "updated@market.com",
                3
        );

        given(vendorRepository.findById(invalidVendorId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> vendorService.updateVendor(invalidVendorId, request))
                .isInstanceOf(VendorException.class)
                .extracting("errorModel")
                .isEqualTo(VendorErrorCode.VENDOR_NOT_FOUND);

        verify(vendorRepository).findById(invalidVendorId);
    }

    @Test
    @DisplayName("거래처 비활성화 성공")
    void givenVendorId_whenDeactivateVendor_thenSuccess() {
        // given
        Long vendorId = 1L;
        Store store = Store.create("청춘식당", "1234567890");
        Vendor vendor = Vendor.create(store, "신선마트", "김철수", "010-1234-5678", "fresh@market.com", 2);

        given(vendorRepository.findById(vendorId)).willReturn(Optional.of(vendor));

        // when
        vendorService.deactivateVendor(vendorId);

        // then
        assertThat(vendor.getStatus()).isEqualTo(VendorStatus.INACTIVE);

        verify(vendorRepository).findById(vendorId);
    }

    @Test
    @DisplayName("거래처 비활성화 실패 - 거래처 없음")
    void givenInvalidVendorId_whenDeactivateVendor_thenThrowException() {
        // given
        Long invalidVendorId = 999L;

        given(vendorRepository.findById(invalidVendorId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> vendorService.deactivateVendor(invalidVendorId))
                .isInstanceOf(VendorException.class)
                .extracting("errorModel")
                .isEqualTo(VendorErrorCode.VENDOR_NOT_FOUND);

        verify(vendorRepository).findById(invalidVendorId);
    }
}

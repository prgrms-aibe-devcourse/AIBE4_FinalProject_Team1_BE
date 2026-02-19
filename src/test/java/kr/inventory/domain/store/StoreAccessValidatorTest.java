package kr.inventory.domain.store;

import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StoreAccessValidatorTest {

    @Mock
    private StoreMemberRepository storeMemberRepository;

    @InjectMocks
    private StoreAccessValidator storeAccessValidator;

    @Test
    @DisplayName("사용자 ID와 매장 Public ID가 유효하면 매장 PK를 반환한다.")
    void validateAndGetStoreId_Success() {
        // given
        Long userId = 1L;
        UUID publicId = UUID.randomUUID();
        Long expectedStoreId = 100L;

        given(storeMemberRepository.findStoreIdByUserAndPublicId(userId, publicId))
                .willReturn(Optional.of(expectedStoreId));

        // when
        Long resultId = storeAccessValidator.validateAndGetStoreId(userId, publicId);

        // then
        assertThat(resultId).isEqualTo(expectedStoreId);
        verify(storeMemberRepository, times(1)).findStoreIdByUserAndPublicId(userId, publicId);
    }

    @Test
    @DisplayName("매장이 존재하지 않거나 권한이 없으면 StoreException이 발생한다.")
    void validateAndGetStoreId_ThrowsException() {
        // given
        Long userId = 1L;
        UUID publicId = UUID.randomUUID();

        given(storeMemberRepository.findStoreIdByUserAndPublicId(userId, publicId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeAccessValidator.validateAndGetStoreId(userId, publicId))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining(StoreErrorCode.STORE_NOT_FOUND_OR_ACCESS_DENIED.getMessage());
    }
}
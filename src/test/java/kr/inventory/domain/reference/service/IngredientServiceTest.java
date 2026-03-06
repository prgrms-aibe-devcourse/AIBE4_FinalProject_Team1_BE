package kr.inventory.domain.reference.service;

import kr.inventory.domain.reference.controller.dto.request.IngredientCreateRequest;
import kr.inventory.domain.reference.controller.dto.request.IngredientUpdateRequest;
import kr.inventory.domain.reference.controller.dto.response.IngredientResponse;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.reference.exception.IngredientErrorCode;
import kr.inventory.domain.reference.exception.IngredientException;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.normalization.model.InboundSpecExtractor;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreAccessValidator storeAccessValidator;
    @Mock
    private InboundSpecExtractor specExtractor;

    @InjectMocks
    private IngredientService ingredientService;

    private final Long userId = 1L;
    private final Long storeId = 100L;
    private final UUID storePublicId = UUID.randomUUID();
    private final UUID ingredientPublicId = UUID.randomUUID();

    @Test
    @DisplayName("식재료 생성 성공")
    void createIngredient_Success() {
        // given
        IngredientCreateRequest request =
                new IngredientCreateRequest("소금", IngredientUnit.G, BigDecimal.valueOf(3));
        Store store = mock(Store.class);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(storeId);
        given(storeRepository.findById(storeId))
                .willReturn(Optional.of(store));

        given(specExtractor.extract(anyString()))
                .willReturn(Optional.empty());

        // when
        IngredientResponse response = ingredientService.createIngredient(userId, storePublicId, request);

        // then
        assertThat(response).isNotNull();
        verify(ingredientRepository, times(1)).save(any(Ingredient.class));
    }

    @Test
    @DisplayName("단일 식재료 조회 성공 - storeId + publicId로 유효 엔티티 조회")
    void getIngredient_Success() {
        // given
        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(storeId);

        Ingredient ingredient = mock(Ingredient.class);

        given(ingredientRepository.findByIngredientPublicIdAndStoreStoreIdAndStatusNot(
                ingredientPublicId,
                storeId,
                IngredientStatus.DELETED
        )).willReturn(Optional.of(ingredient));

        // when
        IngredientResponse response = ingredientService.getIngredient(userId, storePublicId, ingredientPublicId);

        // then
        assertThat(response).isNotNull();
        verify(ingredientRepository).findByIngredientPublicIdAndStoreStoreIdAndStatusNot(
                ingredientPublicId,
                storeId,
                IngredientStatus.DELETED
        );
    }

    @Test
    @DisplayName("식재료 조회 실패 - 존재하지 않거나 삭제 상태/상점 불일치면 NOT_FOUND")
    void getIngredient_Fail_NotFound() {
        // given
        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(storeId);

        given(ingredientRepository.findByIngredientPublicIdAndStoreStoreIdAndStatusNot(
                ingredientPublicId,
                storeId,
                IngredientStatus.DELETED
        )).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> ingredientService.getIngredient(userId, storePublicId, ingredientPublicId))
                .isInstanceOf(IngredientException.class)
                .extracting("errorModel")
                .isEqualTo(IngredientErrorCode.INGREDIENT_NOT_FOUND);
    }

    @Test
    @DisplayName("식재료 수정 성공")
    void updateIngredient_Success() {
        // given
        IngredientUpdateRequest request =
                new IngredientUpdateRequest("계란", IngredientUnit.EA, BigDecimal.valueOf(5), IngredientStatus.ACTIVE);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(storeId);

        Ingredient ingredient = mock(Ingredient.class);

        given(ingredientRepository.findByIngredientPublicIdAndStoreStoreIdAndStatusNot(
                ingredientPublicId,
                storeId,
                IngredientStatus.DELETED
        )).willReturn(Optional.of(ingredient));

        // when
        IngredientResponse response = ingredientService.updateIngredient(userId, storePublicId, ingredientPublicId, request);

        // then
        assertThat(response).isNotNull();
        verify(ingredient).update(
                eq(request.name()),
                eq(request.unit()),
                eq(request.lowStockThreshold()),
                eq(request.status())
        );
    }

    @Test
    @DisplayName("식재료 삭제 성공 - soft delete(ingredient.delete 호출)")
    void deleteIngredient_Success() {
        // given
        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(storeId);

        Ingredient ingredient = mock(Ingredient.class);

        given(ingredientRepository.findByIngredientPublicIdAndStoreStoreIdAndStatusNot(
                ingredientPublicId,
                storeId,
                IngredientStatus.DELETED
        )).willReturn(Optional.of(ingredient));

        // when
        ingredientService.deleteIngredient(userId, storePublicId, ingredientPublicId);

        // then
        verify(ingredient).delete();
        verify(ingredientRepository, never()).delete(any());
    }
}
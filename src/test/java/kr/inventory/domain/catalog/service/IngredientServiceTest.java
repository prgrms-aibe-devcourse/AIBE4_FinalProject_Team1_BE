package kr.inventory.domain.catalog.service;

import kr.inventory.domain.catalog.controller.dto.IngredientCreateRequest;
import kr.inventory.domain.catalog.controller.dto.IngredientResponse;
import kr.inventory.domain.catalog.controller.dto.IngredientUpdateRequest;
import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.catalog.entity.enums.IngredientStatus;
import kr.inventory.domain.catalog.entity.enums.IngredientUnit;
import kr.inventory.domain.catalog.exception.IngredientErrorCode;
import kr.inventory.domain.catalog.exception.IngredientException;
import kr.inventory.domain.catalog.repository.IngredientRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreAccessValidator storeAccessValidator;

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
        IngredientCreateRequest request = new IngredientCreateRequest("소금", IngredientUnit.KG, BigDecimal.valueOf(3));
        Store store = Mockito.mock(Store.class);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        // when
        UUID resultId = ingredientService.createIngredient(userId, storePublicId, request);

        // then
        verify(ingredientRepository, times(1)).save(any(Ingredient.class));
    }

    @Test
    @DisplayName("단일 식재료 조회 성공 - 해당 상점 소속일 때")
    void getIngredient_Success() {
        // given
        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        Store store = Mockito.mock(Store.class);
        given(store.getStoreId()).willReturn(storeId); // 상점 ID 일치 설정

        Ingredient ingredient = Mockito.mock(Ingredient.class);
        given(ingredient.getStore()).willReturn(store);
        given(ingredientRepository.findByIngredientPublicId(ingredientPublicId)).willReturn(Optional.of(ingredient));

        // when
        IngredientResponse response = ingredientService.getIngredient(userId, storePublicId, ingredientPublicId);

        // then
        assertThat(response).isNotNull();
        verify(ingredientRepository).findByIngredientPublicId(ingredientPublicId);
    }

    @Test
    @DisplayName("식재료 조회 실패 - 상점 소속이 아닐 경우 예외 발생")
    void getIngredient_Fail_StoreMismatch() {
        // given
        Long anotherStoreId = 200L;
        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        Store anotherStore = Mockito.mock(Store.class);
        given(anotherStore.getStoreId()).willReturn(anotherStoreId);

        Ingredient ingredient = Mockito.mock(Ingredient.class);
        given(ingredient.getStore()).willReturn(anotherStore);
        given(ingredientRepository.findByIngredientPublicId(ingredientPublicId)).willReturn(Optional.of(ingredient));

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
        IngredientUpdateRequest request = new IngredientUpdateRequest("계란", IngredientUnit.EA, BigDecimal.valueOf(5), IngredientStatus.ACTIVE);
        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        Store store = Mockito.mock(Store.class);
        given(store.getStoreId()).willReturn(storeId);

        Ingredient ingredient = Mockito.mock(Ingredient.class);
        given(ingredient.getStore()).willReturn(store);
        given(ingredientRepository.findByIngredientPublicId(ingredientPublicId)).willReturn(Optional.of(ingredient));

        // when
        ingredientService.updateIngredient(userId, storePublicId, ingredientPublicId, request);

        // then
        verify(ingredient).update(
                anyString(),
                any(IngredientUnit.class),
                any(BigDecimal.class),
                any(IngredientStatus.class)
        );
    }

    @Test
    @DisplayName("식재료 삭제 성공")
    void deleteIngredient_Success() {
        // given
        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        Store store = Mockito.mock(Store.class);
        given(store.getStoreId()).willReturn(storeId);

        Ingredient ingredient = Mockito.mock(Ingredient.class);
        given(ingredient.getStore()).willReturn(store);
        given(ingredientRepository.findByIngredientPublicId(ingredientPublicId)).willReturn(Optional.of(ingredient));

        // when
        ingredientService.deleteIngredient(userId, storePublicId, ingredientPublicId);

        // then
        verify(ingredientRepository).delete(ingredient);
    }
}
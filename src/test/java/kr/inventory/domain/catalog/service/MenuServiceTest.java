package kr.inventory.domain.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.catalog.controller.dto.MenuCreateRequest;
import kr.inventory.domain.catalog.controller.dto.MenuResponse;
import kr.inventory.domain.catalog.controller.dto.MenuUpdateRequest;
import kr.inventory.domain.catalog.entity.Menu;
import kr.inventory.domain.catalog.entity.enums.MenuStatus;
import kr.inventory.domain.catalog.exception.MenuErrorCode;
import kr.inventory.domain.catalog.exception.MenuException;
import kr.inventory.domain.catalog.repository.MenuRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreAccessValidator storeAccessValidator;

    @InjectMocks
    private MenuService menuService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Long userId = 1L;
    private final Long storeId = 100L;
    private final UUID storePublicId = UUID.randomUUID();
    private final UUID menuPublicId = UUID.randomUUID();

    @Test
    @DisplayName("메뉴 생성 성공 - 식재료 리스트가 포함된 경우")
    void createMenu_Success() throws JsonProcessingException {
        // given
        // 요청하신 JSON 구조 시뮬레이션
        String ingredientsJson = """
            [
                {"name": "쌀", "unit": "G", "amount": "200"},
                {"name": "계란", "unit": "EA", "amount": "1"}
            ]
            """;
        JsonNode ingredientsNode = objectMapper.readTree(ingredientsJson);

        MenuCreateRequest request = new MenuCreateRequest("볶음밥", BigDecimal.valueOf(8500), ingredientsNode);
        Store store = mock(Store.class);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        // when
        menuService.createMenu(userId, storePublicId, request);

        // then
        verify(menuRepository, times(1)).save(any(Menu.class));
    }

    @Test
    @DisplayName("전체 메뉴 목록 조회 성공")
    void getMenus_Success() {
        // given
        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(menuRepository.findAllByStoreStoreId(storeId)).willReturn(List.of());

        // when
        List<MenuResponse> result = menuService.getMenus(userId, storePublicId);

        // then
        assertThat(result).isNotNull();
        verify(menuRepository).findAllByStoreStoreId(storeId);
    }

    @Test
    @DisplayName("단일 메뉴 조회 성공 - 해당 상점 소속일 때")
    void getMenu_Success() {
        // given
        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        Store store = mock(Store.class);
        given(store.getStoreId()).willReturn(storeId);

        Menu menu = mock(Menu.class);
        given(menu.getStore()).willReturn(store);
        given(menuRepository.findByMenuPublicId(menuPublicId)).willReturn(Optional.of(menu));

        // when
        MenuResponse response = menuService.getMenu(userId, storePublicId, menuPublicId);

        // then
        assertThat(response).isNotNull();
        verify(menuRepository).findByMenuPublicId(menuPublicId);
    }

    @Test
    @DisplayName("메뉴 조회 실패 - 상점 소속이 아닐 경우 예외 발생")
    void getMenu_Fail_StoreMismatch() {
        // given
        Long anotherStoreId = 200L;
        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        Store anotherStore = mock(Store.class);
        given(anotherStore.getStoreId()).willReturn(anotherStoreId);

        Menu menu = mock(Menu.class);
        given(menu.getStore()).willReturn(anotherStore);
        given(menuRepository.findByMenuPublicId(menuPublicId)).willReturn(Optional.of(menu));

        // when & then
        assertThatThrownBy(() -> menuService.getMenu(userId, storePublicId, menuPublicId))
                .isInstanceOf(MenuException.class)
                .extracting("errorModel")
                .isEqualTo(MenuErrorCode.MENU_NOT_FOUND);
    }

    @Test
    @DisplayName("메뉴 수정 성공")
    void updateMenu_Success() {
        // given
        JsonNode emptyIngredients = objectMapper.createObjectNode();
        MenuUpdateRequest request = new MenuUpdateRequest("짜장면", BigDecimal.valueOf(6500), MenuStatus.ACTIVE, emptyIngredients);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        Store store = mock(Store.class);
        given(store.getStoreId()).willReturn(storeId);

        Menu menu = mock(Menu.class);
        given(menu.getStore()).willReturn(store);
        given(menuRepository.findByMenuPublicId(menuPublicId)).willReturn(Optional.of(menu));

        // when
        menuService.updateMenu(userId, storePublicId, menuPublicId, request);

        // then
        verify(menu).update(
                eq(request.name()),
                eq(request.basePrice()),
                eq(request.status()),
                any(JsonNode.class)
        );
    }

    @Test
    @DisplayName("메뉴 삭제 성공")
    void deleteMenu_Success() {
        // given
        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        Store store = mock(Store.class);
        given(store.getStoreId()).willReturn(storeId);

        Menu menu = mock(Menu.class);
        given(menu.getStore()).willReturn(store);
        given(menuRepository.findByMenuPublicId(menuPublicId)).willReturn(Optional.of(menu));

        // when
        menuService.deleteMenu(userId, storePublicId, menuPublicId);

        // then
        verify(menuRepository).delete(menu);
    }
}
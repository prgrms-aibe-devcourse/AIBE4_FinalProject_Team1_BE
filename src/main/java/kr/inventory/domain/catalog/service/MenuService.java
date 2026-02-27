package kr.inventory.domain.catalog.service;

import kr.inventory.domain.catalog.controller.dto.MenuCreateRequest;
import kr.inventory.domain.catalog.controller.dto.MenuResponse;
import kr.inventory.domain.catalog.controller.dto.MenuUpdateRequest;
import kr.inventory.domain.catalog.entity.Menu;
import kr.inventory.domain.catalog.exception.MenuErrorCode;
import kr.inventory.domain.catalog.exception.MenuException;
import kr.inventory.domain.catalog.repository.MenuRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final StoreRepository storeRepository;
    private final StoreAccessValidator storeAccessValidator;

    @Transactional
    public UUID createMenu(Long userId, UUID storePublicId, MenuCreateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        Menu menu = Menu.create(store, request.name(), request.basePrice(), request.ingredientsJson());
        menuRepository.save(menu);
        return menu.getMenuPublicId();
    }

    public List<MenuResponse> getMenus(Long userId, UUID storePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        return menuRepository.findAllByStoreStoreId(storeId).stream()
                .map(MenuResponse::from)
                .toList();
    }

    public MenuResponse getMenu(Long userId, UUID storePublicId, UUID menuPublicId) {
        Menu menu = validateAndGetMenu(userId, storePublicId, menuPublicId);
        return MenuResponse.from(menu);
    }

    @Transactional
    public void updateMenu(Long userId, UUID storePublicId, UUID menuPublicId, MenuUpdateRequest request) {
        Menu menu = validateAndGetMenu(userId, storePublicId, menuPublicId);
        menu.update(request.name(), request.basePrice(), request.status(), request.ingredientsJson());
    }

    @Transactional
    public void deleteMenu(Long userId, UUID storePublicId, UUID menuPublicId) {
        Menu menu = validateAndGetMenu(userId, storePublicId, menuPublicId);
        menuRepository.delete(menu);
    }

    private Menu validateAndGetMenu(Long userId, UUID storePublicId, UUID menuPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Menu menu = menuRepository.findByMenuPublicId(menuPublicId)
                .orElseThrow(() -> new MenuException(MenuErrorCode.MENU_NOT_FOUND));

        if (!menu.getStore().getStoreId().equals(storeId)) {
            throw new MenuException(MenuErrorCode.MENU_ACCESS_DENIED);
        }
        return menu;
    }

    public List<MenuResponse> customerGetMenus(UUID storePublicId) {
        Store store = storeRepository.findByStorePublicId(storePublicId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        Long storeId = store.getStoreId();

        return menuRepository.findAllByStoreStoreId(storeId).stream()
                .map(MenuResponse::from)
                .toList();
    }
}

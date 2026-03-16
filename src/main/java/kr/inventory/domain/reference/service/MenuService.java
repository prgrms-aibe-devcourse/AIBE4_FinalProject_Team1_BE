package kr.inventory.domain.reference.service;

import kr.inventory.domain.reference.controller.dto.request.MenuCreateRequest;
import kr.inventory.domain.reference.controller.dto.request.MenuUpdateRequest;
import kr.inventory.domain.reference.controller.dto.response.MenuResponse;
import kr.inventory.domain.reference.entity.Menu;
import kr.inventory.domain.reference.entity.enums.MenuStatus;
import kr.inventory.domain.reference.exception.MenuErrorCode;
import kr.inventory.domain.reference.exception.MenuException;
import kr.inventory.domain.reference.repository.MenuRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

        validateDuplicateMenuName(store, request.name());

        try{
            Menu menu = Menu.create(store, request.name(), request.basePrice(), request.ingredientsJson());
            menuRepository.save(menu);
            return menu.getMenuPublicId();
        } catch (DataIntegrityViolationException e) {
            throw new MenuException(MenuErrorCode.DUPLICATE_MENU_NAME);
        }
    }

    public List<MenuResponse> getMenus(Long userId, UUID storePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        return menuRepository.findAllByStoreStoreIdAndStatusNot(storeId, MenuStatus.DELETED).stream()
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

        validateDuplicateMenuNameForUpdate(menu, request.name());

        try {
            menu.update(request.name(), request.basePrice(), request.status(), request.ingredientsJson());
        } catch (DataIntegrityViolationException e) {
            throw new MenuException(MenuErrorCode.DUPLICATE_MENU_NAME);
        }
    }

    @Transactional
    public void deleteMenu(Long userId, UUID storePublicId, UUID menuPublicId) {
        Menu menu = validateAndGetMenu(userId, storePublicId, menuPublicId);
        menu.delete();
    }

    private Menu validateAndGetMenu(Long userId, UUID storePublicId, UUID menuPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        // ACTIVE 전용 조회: storeId + publicId + DELETED 제외
        return menuRepository
                .findByMenuPublicIdAndStoreStoreIdAndStatusNot(
                        menuPublicId,
                        storeId,
                        MenuStatus.DELETED
                )
                .orElseThrow(() -> new MenuException(MenuErrorCode.MENU_NOT_FOUND));
    }

    public List<MenuResponse> customerGetMenus(UUID storePublicId) {
        Store store = storeRepository.findByStorePublicId(storePublicId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        Long storeId = store.getStoreId();

        return menuRepository.findAllByStoreStoreIdAndStatusNot(storeId, MenuStatus.DELETED).stream()
                .map(MenuResponse::from)
                .toList();
    }

    private void validateDuplicateMenuName(Store store, String name) {
        boolean exists = menuRepository.existsByStoreAndNameAndStatusNot(
                store,
                name,
                MenuStatus.DELETED
        );

        if (exists) {
            throw new MenuException(MenuErrorCode.DUPLICATE_MENU_NAME);
        }
    }

    private void validateDuplicateMenuNameForUpdate(Menu menu, String name) {
        boolean exists = menuRepository.existsByStoreAndNameAndStatusNotAndMenuIdNot(
                menu.getStore(),
                name,
                MenuStatus.DELETED,
                menu.getMenuId()
        );

        if(exists) {
            throw new MenuException(MenuErrorCode.DUPLICATE_MENU_NAME);
        }
    }
}

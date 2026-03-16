package kr.inventory.domain.store.permission;

import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorePermissionService {

    private final StoreMemberRepository storeMemberRepository;
    private final StoreRolePermissionPolicy storeRolePermissionPolicy;

    public boolean hasPermission(Long userId, UUID storePublicId, PermissionCode permissionCode) {
        log.debug("[Permission] Checking permission - userId: {}, storePublicId: {}, permission: {}",
                userId, storePublicId, permissionCode);

        Optional<StoreMemberRole> roleOpt = storeMemberRepository.findActiveRoleByStorePublicIdAndUserId(storePublicId, userId);

        if (roleOpt.isEmpty()) {
            log.warn("[Permission] No active role found - userId: {}, storePublicId: {}", userId, storePublicId);
            return false;
        }

        StoreMemberRole role = roleOpt.get();
        boolean hasPermission = storeRolePermissionPolicy.hasPermission(role, permissionCode);

        log.debug("[Permission] Role: {}, HasPermission: {}", role, hasPermission);

        return hasPermission;
    }

    public void validatePermission(Long userId, UUID storePublicId, PermissionCode permissionCode) {
        if (!hasPermission(userId, storePublicId, permissionCode)) {
            log.error("[Permission] Permission denied - userId: {}, storePublicId: {}, permission: {}",
                    userId, storePublicId, permissionCode);
            throw new PermissionException(PermissionErrorCode.PERMISSION_DENIED);
        }
    }
}

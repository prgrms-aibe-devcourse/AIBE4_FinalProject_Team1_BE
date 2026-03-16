package kr.inventory.domain.store.permission;

import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class StoreRolePermissionPolicy {

    private final Map<StoreMemberRole, Set<PermissionCode>> permissionMap;

    public StoreRolePermissionPolicy() {
        EnumMap<StoreMemberRole, Set<PermissionCode>> map = new EnumMap<>(StoreMemberRole.class);
        map.put(StoreMemberRole.OWNER, EnumSet.of(
                PermissionCode.INVITE_ISSUE,
                PermissionCode.MEMBER_MANAGE,
                PermissionCode.STORE_DELETE
        ));
        map.put(StoreMemberRole.MEMBER, EnumSet.noneOf(PermissionCode.class));
        this.permissionMap = Collections.unmodifiableMap(map);
    }

    public boolean hasPermission(StoreMemberRole role, PermissionCode permissionCode) {
        return permissionMap.getOrDefault(role, Collections.emptySet()).contains(permissionCode);
    }
}

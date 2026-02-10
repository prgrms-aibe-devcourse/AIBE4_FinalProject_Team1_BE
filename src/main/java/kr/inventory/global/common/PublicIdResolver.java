package kr.inventory.global.common;

import java.util.UUID;

public interface PublicIdResolver {
    Long resolveInternalId(UUID publicId);
}

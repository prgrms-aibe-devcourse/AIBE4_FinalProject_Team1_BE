package kr.dontworry.global.common;

import java.util.UUID;

public interface PublicIdResolver {
    Long resolveInternalId(UUID publicId);
}

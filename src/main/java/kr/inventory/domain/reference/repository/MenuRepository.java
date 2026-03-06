package kr.inventory.domain.reference.repository;

import kr.inventory.domain.reference.entity.Menu;
import kr.inventory.domain.reference.entity.enums.MenuStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    // ACTIVE 전용 조회 (DELETED 제외)
    List<Menu> findAllByStoreStoreIdAndStatusNot(Long storeId, MenuStatus status);

    Optional<Menu> findByMenuPublicIdAndStatusNot(UUID publicId, MenuStatus status);

    // 유효 엔티티 로딩 (storeId + publicId + ACTIVE)
    Optional<Menu> findByMenuPublicIdAndStoreStoreIdAndStatusNot(
        UUID menuPublicId,
        Long storeId,
        MenuStatus status
    );

    List<Menu> findByMenuPublicIdInAndStatusNot(List<UUID> menuPublicIds, MenuStatus status);
}

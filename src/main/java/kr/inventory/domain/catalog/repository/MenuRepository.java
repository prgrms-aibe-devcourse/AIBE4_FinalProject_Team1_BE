package kr.inventory.domain.catalog.repository;

import kr.inventory.domain.catalog.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findAllByStoreStoreId(Long storeId);
    Optional<Menu> findByMenuPublicId(UUID publicId);
    List<Menu> findByMenuPublicIdIn(List<UUID> menuPublicIds);
}

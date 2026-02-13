package kr.inventory.domain.catalog.repository;

import kr.inventory.domain.catalog.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
}

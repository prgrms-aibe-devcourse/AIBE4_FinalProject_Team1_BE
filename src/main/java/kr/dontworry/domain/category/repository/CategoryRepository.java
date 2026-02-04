package kr.dontworry.domain.category.repository;

import kr.dontworry.domain.category.entity.Category;
import kr.dontworry.domain.category.entity.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByLedger_LedgerIdOrderBySortOrderAsc(Long ledgerId);
    List<Category> findByLedger_LedgerIdAndStatusOrderBySortOrderAsc(Long ledgerId, CategoryStatus status);
}
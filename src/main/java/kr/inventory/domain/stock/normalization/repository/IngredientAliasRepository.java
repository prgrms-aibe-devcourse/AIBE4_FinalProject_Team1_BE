package kr.inventory.domain.stock.normalization.repository;

import kr.inventory.domain.reference.entity.IngredientAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredientAliasRepository extends JpaRepository<IngredientAlias, Long> {

}
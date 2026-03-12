package kr.inventory.domain.reference.repository;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, Long>, IngredientRepositoryCustom {

    // ACTIVE 전용 조회
    List<Ingredient> findAllByStoreStoreIdAndIngredientIdInAndStatusNot(
            Long storeId,
            List<Long> ingredientIds,
            IngredientStatus status
    );

    List<Ingredient> findAllByStoreStoreIdAndStatusNot(Long storeId, IngredientStatus status);

    // 유효 엔티티 로딩
    Optional<Ingredient> findByIngredientPublicIdAndStoreStoreIdAndStatusNot(
            UUID ingredientPublicId,
            Long storeId,
            IngredientStatus status
    );

    // normalized_name으로 조회
    Optional<Ingredient> findByStoreStoreIdAndNormalizedNameAndStatusNot(
            Long storeId,
            String normalizedName,
            IngredientStatus status
    );

    List<Ingredient> findAllByStoreStoreIdAndIngredientPublicIdInAndStatusNot(Long storeId, List<UUID> publicIds, IngredientStatus status);

    List<Ingredient> findByIngredientIdIn(List<Long> ingredientIds);
}


package kr.inventory.domain.reference.repository;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, Long>, IngredientRepositoryCustom {

	// ACTIVE 전용 조회 (DELETED 제외)
	List<Ingredient> findAllByStoreStoreIdAndIngredientIdInAndStatusNot(
		Long storeId,
		List<Long> ingredientIds,
		IngredientStatus status
	);

	List<Ingredient> findAllByStoreStoreIdAndStatusNot(Long storeId, IngredientStatus status);

	Optional<Ingredient> findByIngredientPublicIdAndStatusNot(UUID publicId, IngredientStatus status);

	// 유효 엔티티 로딩 (storeId + publicId + ACTIVE)
	Optional<Ingredient> findByIngredientPublicIdAndStoreStoreIdAndStatusNot(
		UUID ingredientPublicId,
		Long storeId,
		IngredientStatus status
	);

	// ID로 조회 (DELETED 제외)
	Optional<Ingredient> findByIngredientIdAndStatusNot(Long ingredientId, IngredientStatus status);

	Optional<Ingredient> findByIngredientIdAndStoreStoreIdAndStatusNot(
		Long ingredientId,
		Long storeId,
		IngredientStatus status
	);

	// 이름으로 조회 (자동 생성 시 중복 체크용)
	Optional<Ingredient> findByStoreStoreIdAndNameAndStatusNot(
		Long storeId,
		String name,
		IngredientStatus status
	);

	// normalized_name으로 조회 (canonical 기준 중복 방지용)
	Optional<Ingredient> findByStoreStoreIdAndNormalizedNameAndStatusNot(
		Long storeId,
		String normalizedName,
		IngredientStatus status
	);

	// normalized_name 일괄 조회 (n-gram 후보 매칭용)
	List<Ingredient> findAllByStoreStoreIdAndNormalizedNameInAndStatusNot(
		Long storeId,
		List<String> normalizedNames,
		IngredientStatus status
	);
}

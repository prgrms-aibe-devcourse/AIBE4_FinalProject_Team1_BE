package kr.inventory.domain.stock.normalization.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.reference.entity.IngredientMapping;
import kr.inventory.domain.reference.entity.enums.MappingStatus;
import kr.inventory.domain.stock.normalization.repository.IngredientMappingRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static kr.inventory.domain.reference.entity.QIngredient.ingredient;
import static kr.inventory.domain.reference.entity.QIngredientMapping.ingredientMapping;

@RequiredArgsConstructor
public class IngredientMappingRepositoryImpl implements IngredientMappingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<IngredientMapping> findActiveStoreLevelMapping(
            Long storeId,
            String normalizedRawKey
    ) {
        IngredientMapping result = queryFactory
                .selectFrom(ingredientMapping)
                .join(ingredientMapping.ingredient, ingredient).fetchJoin()
                .join(ingredient.store).fetchJoin()
                .where(
                        ingredientMapping.store.storeId.eq(storeId),
                        ingredientMapping.normalizedRawKey.eq(normalizedRawKey),
                        ingredientMapping.status.eq(MappingStatus.ACTIVE)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
}

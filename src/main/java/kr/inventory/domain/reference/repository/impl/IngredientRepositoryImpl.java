package kr.inventory.domain.reference.repository.impl;

import static kr.inventory.domain.vendor.entity.QVendor.*;

import java.util.Optional;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.QIngredient;
import kr.inventory.domain.reference.repository.IngredientRepositoryCustom;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IngredientRepositoryImpl implements IngredientRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public Optional<Ingredient> findMostSimilarIngredient(Long storeId, String productName) {
		QIngredient ingredient = QIngredient.ingredient;

		NumberExpression<Double> word_similarity = Expressions.numberTemplate(Double.class,
			"function('word_similarity', {0}, {1})", ingredient.name, productName);

		Ingredient result = queryFactory
			.selectFrom(ingredient)
			.where(
				ingredient.store.storeId.eq(storeId),
				word_similarity.gt(0.5)
			)
			.orderBy(word_similarity.desc())
			.fetchFirst();

		return Optional.ofNullable(result);
	}
}

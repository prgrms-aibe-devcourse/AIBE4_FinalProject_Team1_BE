package kr.inventory.domain.reference.repository.impl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.QIngredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.repository.IngredientRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static kr.inventory.domain.reference.entity.QIngredient.ingredient;
import static kr.inventory.domain.store.entity.QStore.store;

@RequiredArgsConstructor
public class IngredientRepositoryImpl implements IngredientRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public Optional<Ingredient> findMostSimilarIngredient(Long storeId, String productName) {
		QIngredient ingredient = QIngredient.ingredient;

		NumberExpression<Double> wordSimilarity = Expressions.numberTemplate(
			Double.class,
			"function('word_similarity', {0}, {1})",
			ingredient.name,
			productName
		);

		Ingredient result = queryFactory
			.selectFrom(ingredient)
			.where(
				ingredient.store.storeId.eq(storeId),
				ingredient.status.ne(IngredientStatus.DELETED),
				wordSimilarity.gt(0.5)
			)
			.orderBy(wordSimilarity.desc())
			.fetchFirst();

		return Optional.ofNullable(result);
	}

	@Override
	public List<IngredientCandidate> findTopNSimilarIngredients(Long storeId, String normalizedQuery, int limit) {
		QIngredient ingredient = QIngredient.ingredient;

		// score = word_similarity(normalized_name, query)
		NumberExpression<Double> similarityScore = Expressions.numberTemplate(
			Double.class,
			"word_similarity({0}, {1})",
			ingredient.normalizedName,
			Expressions.constant(normalizedQuery)
		);

		// ★ MVP에서는 trigram(%) 필터를 제거하고 word_similarity만으로 후보를 뽑습니다.
		// trigram(%)는 내부적으로 similarity() > 0.3 임계치를 강제하여
		// '국내산 양파', 'cj 백설 밀가루' 같은 케이스가 후보 0개가 되기 쉽습니다.
		BooleanExpression minScoreFilter = Expressions.booleanTemplate(
			"word_similarity({0}, {1}) >= {2}",
			ingredient.normalizedName,
			Expressions.constant(normalizedQuery),
			Expressions.constant(0.10) // 기존 0.20보다 완화
		);

		List<Tuple> results = queryFactory
			.select(ingredient, similarityScore)
			.from(ingredient)
			.where(
				ingredient.store.storeId.eq(storeId),
				ingredient.status.ne(IngredientStatus.DELETED),
				ingredient.normalizedName.isNotNull(),
				minScoreFilter
			)
			.orderBy(similarityScore.desc())
			.limit(limit)
			.fetch();

		return results.stream()
			.map(tuple -> new IngredientCandidate(
				tuple.get(ingredient),
				tuple.get(similarityScore)
			))
			.collect(Collectors.toList());
	}

	@Override
	public Optional<Ingredient> findByIngredientPublicIdAndStatusNotWithStore(UUID publicId, IngredientStatus status) {
		Ingredient result = queryFactory
			.selectFrom(ingredient)
			.join(ingredient.store, store).fetchJoin()
			.where(
				ingredient.ingredientPublicId.eq(publicId),
				ingredient.status.ne(status)
			)
			.fetchOne();

		return Optional.ofNullable(result);
	}

    @Override
    public Page<Ingredient> searchByStoreIdAndName(Long storeId, String name, IngredientStatus excludedStatus, Pageable pageable) {
        List<Ingredient> content = queryFactory
                .selectFrom(ingredient)
                .where(
                        ingredient.store.storeId.eq(storeId),
                        ingredient.status.ne(excludedStatus),
                        nameContains(name)
                )
                .orderBy(ingredient.name.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(ingredient.count())
                .from(ingredient)
                .where(
                        ingredient.store.storeId.eq(storeId),
                        ingredient.status.ne(excludedStatus),
                        nameContains(name)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public Optional<Ingredient> findOneByKeyword(Long storeId, String keyword) {
        Ingredient result = queryFactory
                .selectFrom(ingredient)
                .where(
                        ingredient.store.storeId.eq(storeId),
                        nameContains(keyword)
                )
                .orderBy(ingredient.createdAt.asc())
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    private BooleanExpression nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return ingredient.name.containsIgnoreCase(name.trim());
    }
}

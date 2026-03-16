package kr.inventory.domain.analytics.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import kr.inventory.domain.analytics.document.stock.StockShortageDocument;
import kr.inventory.domain.analytics.repository.StockShortageSearchRepository;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.entity.StockShortage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockShortageIndexingService {
	private final StockShortageSearchRepository stockShortageSearchRepository;
	private final IngredientRepository ingredientRepository;

	public void index(List<StockShortage> shortages) {
		if (shortages.isEmpty())
			return;

		Set<Long> ingredientIds = shortages.stream()
			.map(StockShortage::getIngredientId)
			.collect(Collectors.toSet());

		Map<Long, String> ingredientNameMap = ingredientRepository.findAllById(ingredientIds)
			.stream()
			.collect(Collectors.toMap(Ingredient::getIngredientId, Ingredient::getName));

		List<StockShortageDocument> docs = shortages.stream()
			.map(shortage -> StockShortageDocument.from(
				shortage,
				ingredientNameMap.getOrDefault(shortage.getIngredientId(), "알 수 없는 식재료")
			))
			.toList();

		stockShortageSearchRepository.saveAll(docs);
	}
}

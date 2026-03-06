package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockTake;
import kr.inventory.domain.stock.entity.StockTakeSheet;

import java.util.List;
import java.util.UUID;

public interface StockTakeRepositoryCustom {
    List<StockTake> findAllBySheetAndIngredientPublicIdsWithLock(
            StockTakeSheet sheet,
            List<UUID> ingredientPublicIds
    );
}

package kr.inventory.domain.stock.service.command;

import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.user.entity.User;

public record StockTakeConfirmContext(
        Long storeId,
        Store store,
        User user,
        Long sheetId
) {}

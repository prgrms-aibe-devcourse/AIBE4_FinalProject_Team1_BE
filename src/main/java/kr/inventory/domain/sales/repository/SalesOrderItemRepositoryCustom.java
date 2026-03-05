package kr.inventory.domain.sales.repository;

import java.util.List;
import java.util.Map;

public interface SalesOrderItemRepositoryCustom {
    Map<Long, Long> countItemsBySalesOrderIds(List<Long> salesOrderIds);
}

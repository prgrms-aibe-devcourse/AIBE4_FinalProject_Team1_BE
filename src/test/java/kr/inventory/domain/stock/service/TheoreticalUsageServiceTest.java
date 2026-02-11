package kr.inventory.domain.stock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.catalog.entity.Menu;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TheoreticalUsageServiceTest {

    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TheoreticalUsageService theoreticalUsageService;

    @Test
    @DisplayName("복수 아이템 주문 시 재료별 총 소모량이 정확히 합산되어야 한다.")
    void calculateOrderUsage_SummationSuccess() {
        // given
        Long salesOrderId = 1L;
        SalesOrder salesOrder = Mockito.mock(SalesOrder.class);
        when(salesOrder.getSalesOrderId()).thenReturn(salesOrderId);

        List<Map<String, Object>> recipeJson = List.of(
                Map.of("ingredientId", 100L, "qty", new BigDecimal("20.000"), "unit", "g"),
                Map.of("ingredientId", 200L, "qty", new BigDecimal("200.000"), "unit", "ml")
        );

        SalesOrderItem item1 = createOrderItem(1L, 2, recipeJson);
        SalesOrderItem item2 = createOrderItem(2L, 3, recipeJson);

        when(salesOrderItemRepository.findBySalesOrderId(salesOrderId))
                .thenReturn(List.of(item1, item2));

        // when
        Map<Long, BigDecimal> result = theoreticalUsageService.calculateOrderUsage(salesOrder);

        // then
        assertThat(result.get(100L)).isEqualByComparingTo("100");
        assertThat(result.get(200L)).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("메뉴 정보가 없는 아이템이 포함된 경우 RECIPE_NOT_FOUND 예외를 발생시킨다.")
    void calculateOrderUsage_ThrowsExceptionWhenMenuIsNull() {
        // given
        SalesOrder salesOrder = Mockito.mock(SalesOrder.class);
        SalesOrderItem invalidItem = BeanUtils.instantiateClass(SalesOrderItem.class);

        when(salesOrderItemRepository.findBySalesOrderId(any()))
                .thenReturn(List.of(invalidItem));

        // when & then
        assertThatThrownBy(() -> theoreticalUsageService.calculateOrderUsage(salesOrder))
                .isInstanceOf(StockException.class)
                .extracting("errorModel")
                .isEqualTo(StockErrorCode.RECIPE_NOT_FOUND);
    }

    private SalesOrderItem createOrderItem(Long menuId, int quantity, Object jsonContent) {
        SalesOrderItem item = BeanUtils.instantiateClass(SalesOrderItem.class);
        Menu menu = BeanUtils.instantiateClass(Menu.class);

        // [수정] List를 JsonNode로 변환하여 타입 불일치 해결
        JsonNode jsonNode = objectMapper.valueToTree(jsonContent);

        ReflectionTestUtils.setField(menu, "menuId", menuId);
        ReflectionTestUtils.setField(menu, "ingredientsJson", jsonNode);

        ReflectionTestUtils.setField(item, "menu", menu);
        ReflectionTestUtils.setField(item, "quantity", quantity);

        return item;
    }
}
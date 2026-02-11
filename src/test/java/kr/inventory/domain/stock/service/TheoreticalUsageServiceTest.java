package kr.inventory.domain.stock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.catalog.entity.Menu;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TheoreticalUsageServiceTest {

    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TheoreticalUsageService theoreticalUsageService;

    @Test
    @DisplayName("정상적인 주문 아이템들에 대해 통합 이론 소모량을 계산한다")
    void calculateOrderUsage_Success() {
        // given
        Long orderId = 1L;
        SalesOrder salesOrder = mock(SalesOrder.class);
        when(salesOrder.getSalesOrderId()).thenReturn(orderId);

        JsonNode mockJsonNode1 = mock(JsonNode.class);
        JsonNode mockJsonNode2 = mock(JsonNode.class);

        SalesOrderItem item1 = createMockItem(2, mockJsonNode1);
        SalesOrderItem item2 = createMockItem(3, mockJsonNode2);

        when(salesOrderItemRepository.findBySalesOrderId(orderId)).thenReturn(List.of(item1, item2));

        setupMockRecipe(mockJsonNode1, List.of(
                new TheoreticalUsageService.RecipeItem(101L, new BigDecimal("1.5"), "g"),
                new TheoreticalUsageService.RecipeItem(102L, new BigDecimal("0.5"), "g")
        ));
        setupMockRecipe(mockJsonNode2, List.of(
                new TheoreticalUsageService.RecipeItem(101L, new BigDecimal("1.0"), "g")
        ));

        // when
        Map<Long, BigDecimal> result = theoreticalUsageService.calculateOrderUsage(salesOrder);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(101L)).isEqualByComparingTo("6.0"); // (1.5*2) + (1.0*3)
        assertThat(result.get(102L)).isEqualByComparingTo("1.0"); // (0.5*2)
    }

    @Test
    @DisplayName("메뉴 정보가 없는 아이템은 계산에서 제외한다")
    void calculateOrderUsage_SkipInvalidItems() {
        // given
        SalesOrder salesOrder = mock(SalesOrder.class);
        SalesOrderItem invalidItem = mock(SalesOrderItem.class);
        when(invalidItem.getMenu()).thenReturn(null);

        when(salesOrderItemRepository.findBySalesOrderId(any())).thenReturn(List.of(invalidItem));

        // when
        Map<Long, BigDecimal> result = theoreticalUsageService.calculateOrderUsage(salesOrder);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(objectMapper);
    }

    // --- Helper Methods ---

    private SalesOrderItem createMockItem(int quantity, JsonNode jsonNode) {
        SalesOrderItem item = mock(SalesOrderItem.class);
        Menu menu = mock(Menu.class);

        when(item.getQuantity()).thenReturn(quantity);
        when(item.getMenu()).thenReturn(menu);
        when(menu.getIngredientsJson()).thenReturn(jsonNode);
        return item;
    }

    private void setupMockRecipe(JsonNode source, List<TheoreticalUsageService.RecipeItem> mockOutput) {
        when(objectMapper.convertValue(eq(source), any(TypeReference.class)))
                .thenReturn(mockOutput);
    }
}
package kr.inventory.domain.stock.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.Menu;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("이론 소모량 계산 서비스 테스트")
class TheoreticalUsageServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TheoreticalUsageService theoreticalUsageService;

    private final Long storeId = 100L;

    // 유효한 UUID 상수 설정
    private final UUID publicId1 = UUID.randomUUID();
    private final UUID publicId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // 정의되지 않은 필드(name 등)가 있어도 에러를 던지지 않도록 설정 (핵심!)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @DisplayName("복수 아이템 주문 시 재료별 총 소모량이 정확히 합산되어야 한다.")
    void calculateOrderUsage_SummationSuccess() {
        // given
        // 1. 실제 JSON 구조와 일치하는 데이터 구성 (qty는 문자열, 정의되지 않은 name 포함)
        List<Map<String, Object>> recipeJson = List.of(
                Map.of(
                        "qty", "20.000",
                        "name", "가래떡", // RecipeItem에는 없는 필드
                        "unit", "G",
                        "ingredientPublicId", publicId1.toString()
                ),
                Map.of(
                        "qty", "200.000",
                        "name", "진라면",
                        "unit", "EA",
                        "ingredientPublicId", publicId2.toString()
                )
        );

        SalesOrderItem item1 = createOrderItem(1L, 2, recipeJson);
        SalesOrderItem item2 = createOrderItem(2L, 3, recipeJson);
        List<SalesOrderItem> items = List.of(item1, item2);

        // 2. Repository 모킹 (UUID를 내부 Long ID로 변환하기 위함)
        Ingredient ing1 = mock(Ingredient.class);
        Ingredient ing2 = mock(Ingredient.class);

        given(ing1.getIngredientPublicId()).willReturn(publicId1);
        given(ing1.getIngredientId()).willReturn(100L);
        given(ing2.getIngredientPublicId()).willReturn(publicId2);
        given(ing2.getIngredientId()).willReturn(200L);

        given(ingredientRepository.findAllByStoreStoreIdAndIngredientPublicIdIn(eq(storeId), anyList()))
                .willReturn(List.of(ing1, ing2));

        // when
        Map<Long, BigDecimal> result = theoreticalUsageService.calculateOrderUsage(storeId, items);

        // then
        // 재료 100L: 20 * 2 + 20 * 3 = 100
        assertThat(result.get(100L)).isEqualByComparingTo("100");
        // 재료 200L: 200 * 2 + 200 * 3 = 1000
        assertThat(result.get(200L)).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("파싱 가능한 형식이 아닐 경우 RECIPE_PARSE_ERROR 예외를 발생시킨다.")
    void calculateOrderUsage_ThrowsParseException() {
        // given
        // 리스트 형식이 아닌 잘못된 JSON 데이터
        Object invalidJson = Map.of("wrong_structure", "data");
        SalesOrderItem item = createOrderItem(1L, 1, invalidJson);

        // when & then
        assertThatThrownBy(() -> theoreticalUsageService.calculateOrderUsage(storeId, List.of(item)))
                .isInstanceOf(StockException.class)
                .extracting("errorModel")
                .isEqualTo(StockErrorCode.RECIPE_PARSE_ERROR);
    }

    private SalesOrderItem createOrderItem(Long menuId, int quantity, Object jsonContent) {
        SalesOrderItem item = BeanUtils.instantiateClass(SalesOrderItem.class);
        Menu menu = BeanUtils.instantiateClass(Menu.class);

        // Map/List 객체를 JsonNode로 변환하여 주입
        JsonNode jsonNode = objectMapper.valueToTree(jsonContent);

        ReflectionTestUtils.setField(menu, "menuId", menuId);
        ReflectionTestUtils.setField(menu, "ingredientsJson", jsonNode);

        ReflectionTestUtils.setField(item, "menu", menu);
        ReflectionTestUtils.setField(item, "quantity", quantity);

        return item;
    }
}
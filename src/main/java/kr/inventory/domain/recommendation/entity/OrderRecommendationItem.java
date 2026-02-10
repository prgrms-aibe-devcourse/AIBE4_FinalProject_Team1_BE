package kr.inventory.domain.recommendation.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.common.CreatedAtEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(
        name = "order_recommendation_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ori_recommendation_ingredient",
                columnNames = {"recommendation_id", "ingredient_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderRecommendationItem extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendationItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private OrderRecommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal recommendedQty;

    @Column(precision = 14, scale = 3)
    private BigDecimal adjustedQty;

    @Column(precision = 3, scale = 2)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode reasonCodes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode evidenceJson;

    public static OrderRecommendationItem create(
            OrderRecommendation recommendation,
            Ingredient ingredient,
            BigDecimal recommendedQty
    ) {
        OrderRecommendationItem item = new OrderRecommendationItem();
        item.recommendation = recommendation;
        item.ingredient = ingredient;
        item.recommendedQty = recommendedQty;
        return item;
    }
}

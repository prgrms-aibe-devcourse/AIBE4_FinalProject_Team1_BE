package kr.inventory.domain.catalog.entity;

import jakarta.persistence.*;
import kr.inventory.domain.catalog.entity.enums.IngredientStatus;
import kr.inventory.domain.catalog.entity.enums.IngredientUnit;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.store.entity.Store;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "ingredients",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "name"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ingredient extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ingredientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngredientUnit unit;

    @Column(precision = 14, scale = 3)
    private BigDecimal lowStockThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngredientStatus status;

    public static Ingredient create(Store store, String name, IngredientUnit unit) {
        Ingredient ingredient = new Ingredient();
        ingredient.store = store;
        ingredient.name = name;
        ingredient.unit = unit;
        ingredient.status = IngredientStatus.ACTIVE;
        return ingredient;
    }
}

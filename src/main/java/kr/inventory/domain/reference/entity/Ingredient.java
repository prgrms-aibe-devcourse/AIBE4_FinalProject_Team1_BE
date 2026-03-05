package kr.inventory.domain.reference.entity;

import jakarta.persistence.*;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.global.util.IngredientNameNormalizer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "ingredients",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"store_id", "name"}),
                @UniqueConstraint(columnNames = {"store_id", "normalized_name"})
        },
        indexes = {
                @Index(name = "idx_ingredients_store_id", columnList = "store_id"),
                @Index(name = "idx_ingredients_normalized_name", columnList = "normalized_name")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ingredient extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ingredientId;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID ingredientPublicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 200)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngredientUnit unit;

    @Column(precision = 14, scale = 3)
    private BigDecimal lowStockThreshold;

    @Column(precision = 14, scale = 3)
    private BigDecimal unitSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngredientStatus status;

    public static Ingredient create(Store store, String name, IngredientUnit unit, BigDecimal lowStockThreshold) {
        Ingredient ingredient = new Ingredient();
        ingredient.store = store;
        ingredient.name = name;
        ingredient.normalizedName = IngredientNameNormalizer.normalizeForSearch(name);
        ingredient.unit = unit;
        ingredient.lowStockThreshold = lowStockThreshold;
        ingredient.unitSize = null;
        ingredient.status = IngredientStatus.ACTIVE;
        return ingredient;
    }

    public static Ingredient create(Store store, String name, IngredientUnit unit, BigDecimal lowStockThreshold, BigDecimal unitSize) {
        Ingredient ingredient = new Ingredient();
        ingredient.store = store;
        ingredient.name = name;
        ingredient.normalizedName = IngredientNameNormalizer.normalizeForSearch(name);
        ingredient.unit = unit;
        ingredient.lowStockThreshold = lowStockThreshold;
        ingredient.unitSize = unitSize;
        ingredient.status = IngredientStatus.ACTIVE;
        return ingredient;
    }

    public void update(String name, IngredientUnit unit, BigDecimal lowStockThreshold, IngredientStatus status) {
        this.name = name;
        this.normalizedName = IngredientNameNormalizer.normalizeForSearch(name);
        this.unit = unit;
        this.lowStockThreshold = lowStockThreshold;
        this.status = status;
    }

    public void update(String name, IngredientUnit unit, BigDecimal lowStockThreshold, IngredientStatus status, BigDecimal unitSize) {
        this.name = name;
        this.normalizedName = IngredientNameNormalizer.normalizeForSearch(name);
        this.unit = unit;
        this.lowStockThreshold = lowStockThreshold;
        this.unitSize = unitSize;
        this.status = status;
    }

    public void delete() {
        this.status = IngredientStatus.DELETED;
    }
}

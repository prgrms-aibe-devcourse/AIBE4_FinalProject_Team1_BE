package kr.inventory.domain.reference.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.reference.entity.enums.MappingStatus;
import kr.inventory.domain.store.entity.Store;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "ingredient_mappings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"store_id", "normalized_raw_key"})
        },
        indexes = {
                @Index(name = "idx_ingredient_mappings_ingredient_id", columnList = "ingredient_id"),
                @Index(name = "idx_ingredient_mappings_store_id", columnList = "store_id"),
                @Index(name = "idx_ingredient_mappings_store_normalized", columnList = "store_id, normalized_raw_key")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientMapping extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mappingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "normalized_raw_key", nullable = false)
    private String normalizedRawKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MappingStatus status;

    public static IngredientMapping createNormalizedRawMapping(
            Ingredient ingredient,
            Store store,
            String normalizedRawKey
    ) {
        IngredientMapping mapping = new IngredientMapping();
        mapping.ingredient = ingredient;
        mapping.store = store;
        mapping.normalizedRawKey = normalizedRawKey;
        mapping.status = MappingStatus.ACTIVE;
        return mapping;
    }

    public void updateIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }
}

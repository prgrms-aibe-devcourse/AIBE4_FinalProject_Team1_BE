package kr.inventory.domain.reference.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.store.entity.Store;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "ingredient_aliases",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"store_id", "alias"})
        },
        indexes = {
                @Index(name = "idx_ingredient_aliases_store_id", columnList = "store_id"),
                @Index(name = "idx_ingredient_aliases_store_alias", columnList = "store_id, alias"),
                @Index(name = "idx_ingredient_aliases_store_canonical", columnList = "store_id, canonical")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientAlias extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aliasId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 120)
    private String alias;

    @Column(nullable = false, length = 120)
    private String canonical;

    public static IngredientAlias create(Store store, String alias, String canonical) {
        IngredientAlias entity = new IngredientAlias();
        entity.store = store;
        entity.alias = alias;
        entity.canonical = canonical;
        return entity;
    }
}

package kr.inventory.domain.reference.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "ingredient_aliases",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"alias"})
        },
        indexes = {
                @Index(name = "idx_ingredient_aliases_alias", columnList = "alias"),
                @Index(name = "idx_ingredient_aliases_canonical", columnList = "canonical")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientAlias extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aliasId;

    @Column(nullable = false, length = 120)
    private String alias;

    @Column(nullable = false, length = 120)
    private String canonical;

    public static IngredientAlias create(String alias, String canonical) {
        IngredientAlias entity = new IngredientAlias();
        entity.alias = alias;
        entity.canonical = canonical;
        return entity;
    }

    public void updateCanonical(String canonical) {
        this.canonical = canonical;
    }
}
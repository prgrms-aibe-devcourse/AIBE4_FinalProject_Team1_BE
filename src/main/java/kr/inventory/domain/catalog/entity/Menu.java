package kr.inventory.domain.catalog.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.inventory.domain.catalog.entity.enums.MenuStatus;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.store.entity.Store;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(
        name = "menus",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "name"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long menuId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(precision = 14, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MenuStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ingredients_json", columnDefinition = "jsonb")
    private JsonNode ingredientsJson;

    public static Menu create(Store store, String name, BigDecimal basePrice) {
        Menu menu = new Menu();
        menu.store = store;
        menu.name = name;
        menu.basePrice = basePrice;
        menu.status = MenuStatus.ACTIVE;
        return menu;
    }
}

package kr.inventory.domain.reference.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.inventory.domain.reference.entity.enums.MenuStatus;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.store.entity.Store;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "menus",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "name"})
)
@SQLDelete(sql = "UPDATE menus SET status = 'DELETED' WHERE menu_id = ?")
@Where(clause = "status <> 'DELETED'")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long menuId;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID menuPublicId = UUID.randomUUID();

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

    public static Menu create(Store store, String name, BigDecimal basePrice, JsonNode ingredientsJson) {
        Menu menu = new Menu();
        menu.store = store;
        menu.name = name;
        menu.basePrice = basePrice;
        menu.ingredientsJson = ingredientsJson;
        menu.status = MenuStatus.ACTIVE;
        return menu;
    }

    public void update(String name, BigDecimal basePrice, MenuStatus status, JsonNode ingredientsJson) {
        if (name != null) this.name = name;
        if (basePrice != null) this.basePrice = basePrice;
        if (status != null) this.status = status;
        if (ingredientsJson != null) this.ingredientsJson = ingredientsJson;
    }
}

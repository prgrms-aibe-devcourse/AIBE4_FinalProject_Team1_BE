package kr.inventory.domain.stock.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.common.CreatedAtEntity;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;
import kr.inventory.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_inbound_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockInboundItem extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inboundItemId;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID inboundItemPublicId;

    @Column(nullable = false)
    private String rawProductName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inbound_id", nullable = false)
    private StockInbound inbound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(precision = 14, scale = 2)
    private BigDecimal unitCost;

    private LocalDate expirationDate;

    private String normalizedRawKey;

    private String normalizedRawFull;

    private String productDisplayName;

    private String productKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ResolutionStatus resolutionStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode suggestedCandidatesJsonb;

    @Column(length = 500)
    private String specText;

    public static StockInboundItem create(
        StockInbound inbound,
        String rawProductName,
        BigDecimal quantity,
        BigDecimal unitCost,
        LocalDate expirationDate
    ) {
        StockInboundItem item = new StockInboundItem();
        item.inboundItemPublicId = UUID.randomUUID();
        item.inbound = inbound;
        item.rawProductName = rawProductName;
        item.quantity = quantity;
        item.unitCost = unitCost;
        item.expirationDate = expirationDate;
        return item;
    }

    public static StockInboundItem create(
        StockInbound inbound,
        Ingredient ingredient,
        BigDecimal quantity,
        BigDecimal unitCost,
        LocalDate expirationDate
    ) {
        StockInboundItem item = new StockInboundItem();
        item.inboundItemPublicId = UUID.randomUUID();
        item.inbound = inbound;
        item.ingredient = ingredient;
        item.rawProductName = ingredient.getName();
        item.quantity = quantity;
        item.unitCost = unitCost;
        item.expirationDate = expirationDate;
        return item;
    }

    public static StockInboundItem createRaw(
        StockInbound inbound,
        String rawName,
        BigDecimal quantity,
        BigDecimal unitCost,
        LocalDate expirationDate,
        String specText
    ) {
        StockInboundItem item = create(inbound, rawName, quantity, unitCost, expirationDate);
        item.resolutionStatus = null; // 자동 정규화 전까지 미정
        item.specText = specText;
        return item;
    }

    public void updateResolution(
        ResolutionStatus resolutionStatus,
        Ingredient ingredient,
        JsonNode suggestedCandidatesJsonb
    ) {
        this.resolutionStatus = resolutionStatus;
        this.ingredient = ingredient;
        this.suggestedCandidatesJsonb = suggestedCandidatesJsonb;
    }

    public void updateNormalizedKeys(String normalizedRawKey, String normalizedRawFull) {
        this.normalizedRawKey = normalizedRawKey;
        this.normalizedRawFull = normalizedRawFull;
    }

    public void confirmResolution(Ingredient ingredient) {
        this.ingredient = ingredient;
        this.resolutionStatus = ResolutionStatus.CONFIRMED;
        this.suggestedCandidatesJsonb = null;
    }

    public void updateProductName(String productDisplayName, String productKey) {
        this.productDisplayName = productDisplayName;
        this.productKey = productKey;
    }
}

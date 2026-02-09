package kr.dontworry.domain.document.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.catalog.entity.Ingredient;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.document.entity.enums.DocumentOcrItemStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "document_ocr_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentOcrItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentOcrItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, length = 200)
    private String ingredientNameRaw;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_ingredient_id")
    private Ingredient matchedIngredient;

    @Column(precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(precision = 14, scale = 2)
    private BigDecimal unitCost;

    @Column(precision = 14, scale = 2)
    private BigDecimal lineTotal;

    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentOcrItemStatus status;

    @Column(columnDefinition = "text")
    private String notes;

    public static DocumentOcrItem create(Document document, String ingredientNameRaw) {
        DocumentOcrItem item = new DocumentOcrItem();
        item.document = document;
        item.ingredientNameRaw = ingredientNameRaw;
        item.status = DocumentOcrItemStatus.DRAFT;
        return item;
    }
}

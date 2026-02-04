package kr.dontworry.domain.category.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.category.entity.enums.CategoryStatus;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.ledger.entity.Ledger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 100)
    private String icon;

    @Column(length = 30)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryStatus status;

    @Column(nullable = false)
    private Integer sortOrder;

    public static Category create(Ledger ledger, String name, String icon, String color, Integer sortOrder) {
        Category category = new Category();
        category.ledger = ledger;
        category.name = name;
        category.icon = icon;
        category.color = color;
        category.sortOrder = sortOrder;
        category.status = CategoryStatus.ACTIVE;
        return category;
    }
}

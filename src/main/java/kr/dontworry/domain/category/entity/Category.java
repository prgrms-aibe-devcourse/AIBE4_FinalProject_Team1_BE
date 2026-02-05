package kr.dontworry.domain.category.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.category.entity.enums.CategoryStatus;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.ledger.entity.Ledger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("status <> 'DELETED'")
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @Column(nullable = false, unique = true)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 100)
    private String icon;

    @Column(length = 30)
    private String color;

    @Column(nullable = false)
    private boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryStatus status;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column
    private OffsetDateTime deletedAt;

    public static Category createDefault(Ledger ledger, String name, String icon, String color, Integer sortOrder) {
        Category category = new Category();
        category.publicId = UUID.randomUUID();
        category.ledger = ledger;
        category.name = name;
        category.icon = icon;
        category.color = color;
        category.isDefault = true;
        category.sortOrder = sortOrder;
        category.status = CategoryStatus.ACTIVE;
        return category;
    }

    public static Category createCustom(Ledger ledger, String name, String icon, String color, Integer lastSortOrder) {
        Category category = new Category();
        category.publicId = UUID.randomUUID();
        category.ledger = ledger;
        category.name = name;
        category.icon = icon;
        category.color = color;
        category.isDefault = false;
        category.sortOrder = lastSortOrder + 1;
        category.status = CategoryStatus.ACTIVE;
        return category;
    }

    public void update(String name, String icon, String color, Integer sortOrder) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.sortOrder = sortOrder;
    }

    public void activate() {
        this.status = CategoryStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = CategoryStatus.INACTIVE;
    }

    public void remove(){
        this.deletedAt = OffsetDateTime.now(ZoneOffset.UTC);

        if(this.isDefault){
            this.status = CategoryStatus.INACTIVE;
        } else{
          this.status = CategoryStatus.DELETED;
          this.sortOrder = -1;
        }
    }
}
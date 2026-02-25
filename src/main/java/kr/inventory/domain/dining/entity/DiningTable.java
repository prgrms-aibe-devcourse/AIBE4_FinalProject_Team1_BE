package kr.inventory.domain.dining.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.dining.entity.enums.TableStatus;
import kr.inventory.domain.store.entity.Store;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(
        name = "dining_tables",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"store_id", "table_code"})
        },
        indexes = {
                @Index(name = "idx_dining_tables_store", columnList = "store_id"),
                @Index(name = "idx_dining_tables_public_id", columnList = "table_public_id")
        }
)
@SQLDelete(sql = "UPDATE menus SET status = 'DELETED' WHERE menu_id = ?")
@Where(clause = "status <> 'DELETED'")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiningTable extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tableId;

    @Column(name = "table_public_id", nullable = false, updatable = false, unique = true)
    private UUID tablePublicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "table_code", nullable = false, length = 40)
    private String tableCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus status;

    public static DiningTable create(Store store, String tableCode) {
        DiningTable t = new DiningTable();
        t.store = store;
        t.tableCode = tableCode;
        t.status = TableStatus.ACTIVE;
        return t;
    }

    public void update(String tableCode, TableStatus status) {
        if (tableCode != null) this.tableCode = tableCode;
        if (status != null) this.status = status;
    }
}
package kr.inventory.domain.vendor.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.store.entity.Store;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "vendors",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vendors_store_name",
                columnNames = {"store_id", "name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vendor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vendorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 100)
    private String contactPerson;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    private Integer leadTimeDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VendorStatus status;

    public static Vendor create(
            Store store,
            String name,
            String contactPerson,
            String phone,
            String email,
            Integer leadTimeDays
    ) {
        Vendor vendor = new Vendor();
        vendor.store = store;
        vendor.name = name;
        vendor.contactPerson = contactPerson;
        vendor.phone = phone;
        vendor.email = email;
        vendor.leadTimeDays = leadTimeDays != null ? leadTimeDays : 1;
        vendor.status = VendorStatus.ACTIVE;
        return vendor;
    }

    public void updateContactInfo(String contactPerson, String phone, String email) {
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
    }

    public void updateLeadTime(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public void deactivate() {
        this.status = VendorStatus.INACTIVE;
    }

    public void activate() {
        this.status = VendorStatus.ACTIVE;
    }
}
package kr.inventory.domain.store.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.store.entity.enums.StoreStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long storeId;

    @Column(nullable = false, updatable = false)
    private UUID publicId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false, length = 10)
    private String businessRegistrationNumber;

    @Column(length = 200)
    private String address;

    @Column(length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreStatus status;

    public static Store create(String name, String businessRegistrationNumber, String address, String phoneNumber) {
        Store store = new Store();
        store.publicId = UUID.randomUUID();
        store.name = name;
        store.businessRegistrationNumber = businessRegistrationNumber;
        store.address = address;
        store.phoneNumber = phoneNumber;
        store.status = StoreStatus.ACTIVE;
        return store;
    }

    public void updateInfo(String name, String address, String phoneNumber) {
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
    }

    public void updateStatus(StoreStatus newStatus) {
        this.status = newStatus;
    }
}

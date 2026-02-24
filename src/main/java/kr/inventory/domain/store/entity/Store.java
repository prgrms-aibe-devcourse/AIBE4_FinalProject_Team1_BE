package kr.inventory.domain.store.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
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

    @Column(nullable = false, updatable = false, unique = true)
    private UUID storePublicId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false, length = 10)
    private String businessRegistrationNumber;

    public static Store create(String name, String businessRegistrationNumber) {
        Store store = new Store();
        store.storePublicId = UUID.randomUUID();
        store.name = name;
        store.businessRegistrationNumber = businessRegistrationNumber;
        return store;
    }

    public void updateName(String name) {
        this.name = name;
    }
}

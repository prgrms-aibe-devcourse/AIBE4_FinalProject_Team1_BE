package kr.inventory.domain.purchase.service;

import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderItemRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderUpdateRequest;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.purchase.exception.PurchaseOrderException;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PurchaseOrderServiceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("inventory_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreMemberRepository storeMemberRepository;

    @Test
    @DisplayName("발주 상태 전이와 권한 정책이 통합 환경에서 동작한다")
    void purchaseOrderFlow_withRoleAndCrud() {
        Store store = storeRepository.save(Store.create("test-store", "1111222233"));

        User owner = userRepository.save(User.create("owner", "owner@test.com"));
        User member = userRepository.save(User.create("member", "member@test.com"));
        User staff = userRepository.save(User.create("staff", "staff@test.com"));

        storeMemberRepository.save(StoreMember.create(store, owner, StoreMemberRole.OWNER, 1, true));
        storeMemberRepository.save(StoreMember.create(store, member, StoreMemberRole.MEMBER, 2, false));
        storeMemberRepository.save(StoreMember.create(store, staff, StoreMemberRole.MEMBER, 3, false));

        PurchaseOrderDetailResponse created = purchaseOrderService.createDraft(
                owner.getUserId(),
                new PurchaseOrderCreateRequest(
                        store.getStoreId(),
                        List.of(new PurchaseOrderItemRequest("egg", 10, new BigDecimal("500")))
                )
        );

        assertThat(created.status()).isEqualTo(PurchaseOrderStatus.DRAFT);

        PurchaseOrderDetailResponse updated = purchaseOrderService.updateDraft(
                owner.getUserId(),
                created.purchaseOrderId(),
                new PurchaseOrderUpdateRequest(List.of(new PurchaseOrderItemRequest("egg", 12, new BigDecimal("500"))))
        );

        assertThat(updated.items().get(0).quantity()).isEqualTo(12);

        PurchaseOrderDetailResponse submitted = purchaseOrderService.submit(owner.getUserId(), created.purchaseOrderId());
        assertThat(submitted.status()).isEqualTo(PurchaseOrderStatus.SUBMITTED);
        assertThat(submitted.orderNo()).isNotBlank();

        assertThatThrownBy(() -> purchaseOrderService.confirm(member.getUserId(), created.purchaseOrderId()))
                .isInstanceOf(PurchaseOrderException.class);

        PurchaseOrderDetailResponse confirmed = purchaseOrderService.confirm(owner.getUserId(), created.purchaseOrderId());
        assertThat(confirmed.status()).isEqualTo(PurchaseOrderStatus.CONFIRMED);

        PurchaseOrderDetailResponse canceled = purchaseOrderService.cancel(owner.getUserId(), created.purchaseOrderId());
        assertThat(canceled.status()).isEqualTo(PurchaseOrderStatus.CANCELED);

        assertThatThrownBy(() -> purchaseOrderService.getPurchaseOrders(staff.getUserId(), store.getStoreId()))
                .isInstanceOf(PurchaseOrderException.class);
    }
}

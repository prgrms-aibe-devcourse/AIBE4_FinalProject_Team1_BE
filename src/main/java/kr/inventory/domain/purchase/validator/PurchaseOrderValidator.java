package kr.inventory.domain.purchase.validator;

import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderItemRequest;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.purchase.exception.PurchaseOrderErrorCode;
import kr.inventory.domain.purchase.exception.PurchaseOrderException;
import kr.inventory.domain.purchase.repository.PurchaseOrderRepository;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PurchaseOrderValidator {

    private final StoreMemberRepository storeMemberRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    /**
     * 사용자의 발주서 접근 권한을 검증하고 내부 PurchaseOrder ID를 반환.
     * 해당 매장의 모든 활성 멤버가 접근 가능하다.
     */
    public Long validateAccessAndGetPurchaseOrderId(Long userId, UUID purchaseOrderPublicId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByPurchaseOrderPublicId(purchaseOrderPublicId)
                .orElseThrow(() -> new PurchaseOrderException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND));

        Long storeId = purchaseOrder.getStore().getStoreId();

        // 권한 검증: 해당 매장의 활성 멤버이면 접근 가능
        storeMemberRepository.findByStoreStoreIdAndUserUserIdAndStatus(
                storeId,
                userId,
                StoreMemberStatus.ACTIVE)
                .orElseThrow(() -> new PurchaseOrderException(PurchaseOrderErrorCode.PURCHASE_ORDER_ACCESS_DENIED));

        return purchaseOrder.getPurchaseOrderId();
    }

    public void requireItemsNotEmpty(List<PurchaseOrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.EMPTY_ITEMS);
        }
    }

    public void requireCancelable(PurchaseOrderStatus status) {
        if (status == PurchaseOrderStatus.CANCELED) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.ALREADY_CANCELED);
        }
    }
}

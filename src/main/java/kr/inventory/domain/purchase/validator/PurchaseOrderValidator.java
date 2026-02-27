package kr.inventory.domain.purchase.validator;

import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderItemRequest;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.purchase.exception.PurchaseOrderErrorCode;
import kr.inventory.domain.purchase.exception.PurchaseOrderException;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PurchaseOrderValidator {

    private final StoreMemberRepository storeMemberRepository;

    public StoreMemberRole requireManagerOrAbove(Long storeId, Long userId) {
        StoreMember storeMember = storeMemberRepository.findByStoreStoreIdAndUserUserIdAndStatus(
                        storeId,
                        userId,
                        StoreMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new PurchaseOrderException(PurchaseOrderErrorCode.PURCHASE_ORDER_ACCESS_DENIED));

        if (storeMember.getRole() == StoreMemberRole.MEMBER) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.PURCHASE_ORDER_ACCESS_DENIED);
        }

        return storeMember.getRole();
    }

    public void requireOwner(StoreMemberRole role) {
        if (role != StoreMemberRole.OWNER) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.PURCHASE_ORDER_ACCESS_DENIED);
        }
    }

    public void requireItemsNotEmpty(List<PurchaseOrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.EMPTY_ITEMS);
        }
    }

    public void requireDraftForUpdate(PurchaseOrderStatus status) {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.DRAFT_ONLY_MUTATION);
        }
    }

    public void requireDraftForSubmit(PurchaseOrderStatus status) {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    public void requireSubmittedForConfirm(PurchaseOrderStatus status) {
        if (status != PurchaseOrderStatus.SUBMITTED) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    public void requireCancelable(PurchaseOrderStatus status) {
        if (status == PurchaseOrderStatus.CANCELED) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.ALREADY_CANCELED);
        }
    }
}

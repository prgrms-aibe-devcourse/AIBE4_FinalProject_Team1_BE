package kr.inventory.domain.analytics.service.indexing;

import kr.inventory.domain.analytics.document.stock.WasteRecordDocument;
import kr.inventory.domain.analytics.repository.WasteRecordSearchRepository;
import kr.inventory.domain.stock.entity.WasteRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WasteIndexingService {

    private final WasteRecordSearchRepository wasteRecordSearchRepository;

    public void index(WasteRecord wasteRecord) {
        WasteRecordDocument doc = WasteRecordDocument.from(wasteRecord);
        wasteRecordSearchRepository.save(doc);
        log.debug("[ES] 폐기 인덱싱 완료 wasteId={}", wasteRecord.getWasteId());
    }
}

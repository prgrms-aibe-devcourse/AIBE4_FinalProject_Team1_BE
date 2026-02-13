package kr.inventory.domain.stock.controller;

import jakarta.validation.Valid;
import kr.inventory.domain.stock.controller.dto.StocktakeDto;
import kr.inventory.domain.stock.service.StocktakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/stocktakes")
@RequiredArgsConstructor
public class StocktakeController {
    private final StocktakeService stocktakeService;

    @PostMapping
    public ResponseEntity<Long> createSheet(@RequestBody @Valid StocktakeDto.CreateRequest request) {
        return ResponseEntity.ok(stocktakeService.createStocktakeSheet(request));
    }

    @PostMapping("/{sheetId}/confirm")
    public ResponseEntity<Void> confirmSheet(@PathVariable Long sheetId) {
        stocktakeService.confirmSheet(sheetId);
        return ResponseEntity.noContent().build();
    }
}

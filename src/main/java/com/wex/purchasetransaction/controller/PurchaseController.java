package com.wex.purchasetransaction.controller;

import com.wex.purchasetransaction.dto.ConvertedPurchaseResponse;
import com.wex.purchasetransaction.dto.PurchaseTransactionRequest;
import com.wex.purchasetransaction.dto.PurchaseTransactionResponse;
import com.wex.purchasetransaction.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ResponseEntity<PurchaseTransactionResponse> createPurchase(@Valid @RequestBody PurchaseTransactionRequest request) {
        PurchaseTransactionResponse response = purchaseService.savePurchase(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConvertedPurchaseResponse> getConvertedPurchase(
            @PathVariable Long id,
            @RequestParam String targetCountry) {
        ConvertedPurchaseResponse response = purchaseService.getConvertedPurchase(id, targetCountry);
        return ResponseEntity.ok(response);
    }
}

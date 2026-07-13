package com.wex.purchasetransaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseTransactionResponse(
    Long id,
    String description,
    LocalDate transactionDate,
    BigDecimal purchaseAmount
) {}

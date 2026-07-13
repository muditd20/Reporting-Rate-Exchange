package com.wex.purchasetransaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConvertedPurchaseResponse(
    Long id,
    String description,
    LocalDate transactionDate,
    BigDecimal originalUsdAmount,
    BigDecimal exchangeRate,
    BigDecimal convertedAmount
) {}

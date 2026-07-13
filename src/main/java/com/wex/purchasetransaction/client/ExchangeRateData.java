package com.wex.purchasetransaction.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateData(
    @JsonProperty("record_date") LocalDate recordDate,
    @JsonProperty("country") String country,
    @JsonProperty("currency") String currency,
    @JsonProperty("country_currency_desc") String countryCurrencyDesc,
    @JsonProperty("exchange_rate") BigDecimal exchangeRate,
    @JsonProperty("effective_date") LocalDate effectiveDate
) {}

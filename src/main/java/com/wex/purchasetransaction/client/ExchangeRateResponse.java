package com.wex.purchasetransaction.client;

import java.util.List;

public record ExchangeRateResponse(
    List<ExchangeRateData> data
) {}

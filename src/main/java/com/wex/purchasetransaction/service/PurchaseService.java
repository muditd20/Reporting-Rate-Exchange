package com.wex.purchasetransaction.service;

import com.wex.purchasetransaction.client.ExchangeRateData;
import com.wex.purchasetransaction.client.TreasuryExchangeClient;
import com.wex.purchasetransaction.dto.ConvertedPurchaseResponse;
import com.wex.purchasetransaction.dto.PurchaseTransactionRequest;
import com.wex.purchasetransaction.dto.PurchaseTransactionResponse;
import com.wex.purchasetransaction.entity.PurchaseTransaction;
import com.wex.purchasetransaction.repository.PurchaseTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class PurchaseService {

    private final PurchaseTransactionRepository repository;
    private final TreasuryExchangeClient exchangeClient;

    public PurchaseService(PurchaseTransactionRepository repository, TreasuryExchangeClient exchangeClient) {
        this.repository = repository;
        this.exchangeClient = exchangeClient;
    }

    public PurchaseTransactionResponse savePurchase(PurchaseTransactionRequest request) {
        PurchaseTransaction transaction = new PurchaseTransaction(
                request.description(),
                request.transactionDate(),
                request.purchaseAmount().setScale(2, RoundingMode.HALF_UP)
        );

        transaction = repository.save(transaction);

        return new PurchaseTransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getPurchaseAmount()
        );
    }

    public ConvertedPurchaseResponse getConvertedPurchase(Long id, String targetCountry) {
        PurchaseTransaction transaction = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase transaction not found with ID: " + id));

        Optional<ExchangeRateData> exchangeRateDataOpt = exchangeClient.getLatestExchangeRate(targetCountry, transaction.getTransactionDate());

        if (exchangeRateDataOpt.isEmpty()) {
            throw new RuntimeException("Purchase cannot be converted to the target currency: " + targetCountry + " (No exchange rate found)");
        }

        ExchangeRateData exchangeRateData = exchangeRateDataOpt.get();
        LocalDate recordDate = exchangeRateData.recordDate();

        long monthsBetween = ChronoUnit.MONTHS.between(recordDate.withDayOfMonth(1), transaction.getTransactionDate().withDayOfMonth(1));

        if (monthsBetween > 6 || recordDate.isAfter(transaction.getTransactionDate())) {
             throw new RuntimeException("Purchase cannot be converted to the target currency: " + targetCountry + " (No exchange rate available within 6 months equal to or before the purchase date)");
        }

        BigDecimal exchangeRate = exchangeRateData.exchangeRate();
        BigDecimal convertedAmount = transaction.getPurchaseAmount()
                .multiply(exchangeRate)
                .setScale(2, RoundingMode.HALF_UP);

        return new ConvertedPurchaseResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getPurchaseAmount(),
                exchangeRate,
                convertedAmount
        );
    }
}

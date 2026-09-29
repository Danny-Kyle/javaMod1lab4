package com.example.ledger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class SettlementService {

    private final PaymentRepository paymentRepository;
    private final FeeTable feeTable;
    private final BigDecimal defaultFeeRate;

    public SettlementService(PaymentRepository paymentRepository,
                              FeeTable feeTable,
                              @Value("${ledger.fee-rate}") BigDecimal defaultFeeRate) {
        this.paymentRepository = paymentRepository;
        this.feeTable = feeTable;
        this.defaultFeeRate = defaultFeeRate;
    }

    @Transactional
    public PaymentEntity recordPayment(String merchantId, long amountMinor, String currency) {
        PaymentEntity entity = new PaymentEntity(
                UUID.randomUUID().toString(), merchantId, amountMinor, currency, Instant.now());
        return paymentRepository.save(entity);
    }

    /** Fee table is loaded once at startup (see FeeTable). */
    public long amountOwed(String merchantId) {
        List<PaymentEntity> payments = paymentRepository.findByMerchantId(merchantId);
        if (payments.isEmpty()) {
            throw new NoSuchElementException("No payments found for merchant " + merchantId);
        }

        long totalMinor = payments.stream().mapToLong(PaymentEntity::getAmountMinor).sum();
        String currency = payments.get(0).getCurrency();

        BigDecimal rate = feeTable.rateFor(currency, defaultFeeRate);

        BigDecimal gross = BigDecimal.valueOf(totalMinor);
        BigDecimal fee = gross.multiply(rate).setScale(0, RoundingMode.DOWN);
        return gross.subtract(fee).longValueExact();
    }
}

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
    private final BigDecimal defaultFeeRate;

    public SettlementService(PaymentRepository paymentRepository,
                              @Value("${ledger.fee-rate}") BigDecimal defaultFeeRate) {
        this.paymentRepository = paymentRepository;
        this.defaultFeeRate = defaultFeeRate;
    }

    @Transactional
    public PaymentEntity recordPayment(String merchantId, long amountMinor, String currency) {
        PaymentEntity entity = new PaymentEntity(
                UUID.randomUUID().toString(), merchantId, amountMinor, currency, Instant.now());
        return paymentRepository.save(entity);
    }

    public long amountOwed(String merchantId) {
        List<PaymentEntity> payments = paymentRepository.findByMerchantId(merchantId);
        if (payments.isEmpty()) {
            throw new NoSuchElementException("No payments found for merchant " + merchantId);
        }

        long totalMinor = payments.stream().mapToLong(PaymentEntity::getAmountMinor).sum();
        String currency = payments.get(0).getCurrency();

        // First call in the JVM triggers FeeTableHolder's static initializer (the planted defect).
        BigDecimal rate = FeeTableHolder.rateFor(currency, defaultFeeRate);

        BigDecimal gross = BigDecimal.valueOf(totalMinor);
        BigDecimal fee = gross.multiply(rate).setScale(0, RoundingMode.DOWN);
        return gross.subtract(fee).longValueExact();
    }
}

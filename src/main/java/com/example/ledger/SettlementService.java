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
    private final BigDecimal feeRate;

    public SettlementService(PaymentRepository paymentRepository,
                              @Value("${ledger.fee-rate}") BigDecimal feeRate) {
        this.paymentRepository = paymentRepository;
        this.feeRate = feeRate;
    }

    @Transactional
    public PaymentEntity recordPayment(String merchantId, long amountMinor, String currency) {
        PaymentEntity entity = new PaymentEntity(
                UUID.randomUUID().toString(),
                merchantId,
                amountMinor,
                currency,
                Instant.now());
        return paymentRepository.save(entity);
    }

    /**
     * Sum of a merchant's payments, less the ledger.fee-rate fee.
     * The fee is truncated to whole minor units (rounding DOWN) before being
     * subtracted from the gross total, so the merchant is never short-changed
     * by a rounded-up fee.
     */
    public long amountOwed(String merchantId) {
        List<PaymentEntity> payments = paymentRepository.findByMerchantId(merchantId);
        if (payments.isEmpty()) {
            throw new NoSuchElementException("No payments found for merchant " + merchantId);
        }

        long totalMinor = payments.stream()
                .mapToLong(PaymentEntity::getAmountMinor)
                .sum();

        BigDecimal gross = BigDecimal.valueOf(totalMinor);
        BigDecimal fee = gross.multiply(feeRate).setScale(0, RoundingMode.DOWN);
        return gross.subtract(fee).longValueExact();
    }
}

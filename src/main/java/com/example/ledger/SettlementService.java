package com.example.ledger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
     * Same result as before (payment total less the fee, fee truncated), but the total
     * now comes from one SUM query instead of materialising every payment as an object.
     */
    public long amountOwed(String merchantId) {
        Long totalMinor = paymentRepository.sumAmountMinorByMerchantId(merchantId);
        if (totalMinor == null) {
            throw new NoSuchElementException("No payments found for merchant " + merchantId);
        }

        BigDecimal gross = BigDecimal.valueOf(totalMinor);
        BigDecimal fee = gross.multiply(feeRate).setScale(0, RoundingMode.DOWN);
        return gross.subtract(fee).longValueExact();
    }
}

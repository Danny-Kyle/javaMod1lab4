package com.example.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

    List<PaymentEntity> findByMerchantId(String merchantId);

    /**
     * GC lab fix: let the database add the amounts up instead of loading every payment
     * as an entity. Returns null when the merchant has no payments (amount_minor is NOT NULL).
     */
    @Query("select sum(p.amountMinor) from PaymentEntity p where p.merchantId = :merchantId")
    Long sumAmountMinorByMerchantId(@Param("merchantId") String merchantId);
}

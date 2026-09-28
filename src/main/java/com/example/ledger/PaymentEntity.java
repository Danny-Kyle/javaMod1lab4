package com.example.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * This is a plain mutable class, not a record, because it is a JPA entity.
 * Hibernate needs to:
 *   1. instantiate it reflectively via a no-argument constructor and then
 *      populate the fields itself when hydrating a row from the database, and
 *   2. potentially wrap it in a dynamically generated proxy subclass to
 *      support lazy loading and dirty checking.
 * Records are final, have no no-arg constructor, and expose only immutable
 * accessor methods (not field-level mutators), so they cannot be managed
 * as JPA entities the way this class can.
 */
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private String id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected PaymentEntity() {
        // required by JPA/Hibernate for reflective instantiation
    }

    public PaymentEntity(String id, String merchantId, long amountMinor, String currency, Instant recordedAt) {
        this.id = id;
        this.merchantId = merchantId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.recordedAt = recordedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}

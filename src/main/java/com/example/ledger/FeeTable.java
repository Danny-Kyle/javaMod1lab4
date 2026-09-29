package com.example.ledger;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * THE FIX: the table is loaded eagerly, in the constructor of a singleton bean.
 * Spring instantiates it during context refresh, before the embedded server starts
 * accepting traffic, so no request ever blocks on the load. The map is immutable and
 * held in a final field, so it is safely published to every thread.
 */
@Component
public class FeeTable {

    private final Map<String, BigDecimal> table;

    public FeeTable(FeeTableClient client) {
        this.table = Map.copyOf(client.fetchTable());
    }

    public BigDecimal rateFor(String currency, BigDecimal fallback) {
        return table.getOrDefault(currency, fallback);
    }
}

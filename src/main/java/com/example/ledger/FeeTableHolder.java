package com.example.ledger;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PLANTED DEFECT (lab step 6): the blocking downstream call lives in a class initializer.
 * The first request that touches this class runs the static block on its virtual thread;
 * the sleep inside it happens under a class-initialization frame that cannot unmount,
 * so the virtual thread pins its carrier for the whole call.
 */
final class FeeTableHolder {

    private static final Map<String, BigDecimal> TABLE;

    static {
        TABLE = new FeeTableClient(Long.getLong("ledger.downstream.latency-ms", 200L)).fetchTable();
    }

    private FeeTableHolder() {
    }

    static BigDecimal rateFor(String currency, BigDecimal fallback) {
        return TABLE.getOrDefault(currency, fallback);
    }
}

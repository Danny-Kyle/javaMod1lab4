package com.example.ledger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracted so it can be measured in isolation by the JMH benchmark suite.
 * Same string-joining logic ProblemHandler uses to build a validation detail
 * message from a list of "field message" strings — pulled out because
 * ProblemHandler itself depends on Spring MVC types (FieldError) that would
 * otherwise drag Spring onto the benchmarks module's classpath for no reason.
 */
public final class ProblemMessages {

    private ProblemMessages() {
    }

    public static String joinFieldErrors(List<String> fieldMessages) {
        return fieldMessages.stream().collect(Collectors.joining("; "));
    }
}

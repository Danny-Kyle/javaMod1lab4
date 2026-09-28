# Research Notes

## Primary Documentation: How the Datasource Is Pointed at the Container

**Source:** "Testcontainers" — Spring Boot Reference Documentation, version 3.5
**URL:** https://docs.spring.io/spring-boot/3.5/reference/testing/testcontainers.html
**Date read:** September 24, 2026

The docs explain that annotating a `@Container` field with `@ServiceConnection` makes Spring Boot automatically build a connection-details bean from the running container (a `JdbcConnectionDetails` bean, in the case of a `PostgreSQLContainer`), and that bean is what the JDBC auto-configuration then uses to build the `DataSource` — no manual `spring.datasource.url/username/password` properties required. As the documentation states, once a service connection is established, "the connection details take precedence over any connection-related configuration properties."

In practice this is why `PaymentControllerIT` never sets `spring.datasource.*` for the test: the `@ServiceConnection`-annotated `PostgreSQLContainer` field supplies a `JdbcConnectionDetails` bean that overrides the `application.yml` datasource block entirely, pointing the app at the container's mapped host/port instead.

---

## Agent Review: `GET /payments/{id}`

I asked an AI coding agent to add a `GET /payments/{id}` endpoint to the service. Its first draft looked like this (trimmed):

```java
@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping("/{id}")
    public PaymentEntity getPayment(@PathVariable String id) {
        return paymentRepository.findById(id).orElse(null);
    }
}
```

I checked it against the four defects called out in the lab and found all four present:

1. **Field injection instead of constructor injection.** The agent added `@Autowired private PaymentRepository paymentRepository;` directly on `PaymentController`, rather than adding the dependency to the existing constructor. Field injection makes the class harder to unit test (you can't pass a mock through the constructor) and hides a hard dependency behind reflection. **Fix:** inject `PaymentRepository` (or, better, keep routing everything through `SettlementService`) via the controller's existing constructor parameter list.

2. **JPA entity returned as the API type.** The draft returns `PaymentEntity` — the `@Entity` class — straight out of the controller. That leaks persistence concerns (column names, JPA proxy behavior, lazy fields) into the HTTP contract and couples the API shape to the database schema. **Fix:** map to the existing `PaymentResponse` record before returning, the same way `recordPayment` already does.

3. **Missing `@Valid`/constraint annotations.** `id` is a raw `@PathVariable String` with no `@NotBlank` or pattern constraint, so an empty or malformed id would fall through to the repository lookup instead of failing fast with a clear validation error. **Fix:** add `@NotBlank` (with the controller class annotated `@Validated`) or otherwise guard against a blank id before hitting the repository.

4. **Error body that isn't a Problem Detail.** `orElse(null)` means a missing payment silently returns HTTP 200 with a `null` body instead of a 404 with an RFC 9457 problem, breaking the "same error shape on every failure" requirement. **Fix:** throw `NoSuchElementException` (already handled by `ProblemHandler`) when the repository returns empty, e.g. `paymentRepository.findById(id).orElseThrow(() -> new NoSuchElementException("No payment found with id " + id))`, and map the result to `PaymentResponse`.

**Corrected version**, folded into `PaymentController.java`:

```java
@GetMapping("/{id}")
public PaymentResponse getPayment(@PathVariable @NotBlank String id) {
    PaymentEntity entity = settlementService.findById(id)
            .orElseThrow(() -> new NoSuchElementException("No payment found with id " + id));
    return new PaymentResponse(entity.getId(), entity.getMerchantId(), entity.getAmountMinor(), entity.getCurrency());
}
```

(with a corresponding `Optional<PaymentEntity> findById(String id)` added to `SettlementService`, delegating to `paymentRepository.findById(id)`, and `@Validated` added at the class level so the `@NotBlank` on the path variable is enforced).

# Endpoint Log

Run against the app started with the Testcontainers-backed `PaymentControllerIT` (same behavior applies when running locally against a real PostgreSQL instance).

| Request | Status | Body summary | Time |
|---|---|---|---|
| `POST /payments` `{"merchantId":"MR-4471","amountMinor":128450,"currency":"GBP"}` | 201 Created | Returns the created payment: `id`, `merchantId: "MR-4471"`, `amountMinor: 128450`, `currency: "GBP"`. `Location` header set to `/payments/{id}`. | 2026-09-24 09:12:03 |
| `POST /payments` `{"merchantId":"MR-4471","amountMinor":-500,"currency":"GBP"}` | 400 Bad Request | `application/problem+json` body: `type: .../validation-error`, `title: "Validation failed"`, `detail: "amountMinor must be greater than 0"`. | 2026-09-24 09:12:19 |
| `POST /payments` `{"merchantId":"","amountMinor":1000,"currency":"GBP"}` | 400 Bad Request | `application/problem+json` body: `type: .../validation-error`, `title: "Validation failed"`, `detail: "merchantId must not be blank"`. | 2026-09-24 09:12:31 |
| `GET /payments/settlement?merchantId=MR-4471` | 200 OK | `{"merchantId":"MR-4471","amountOwedMinor":124469}` — 128450 minor units less the 3.1% fee. | 2026-09-24 09:12:45 |
| `GET /payments/settlement?merchantId=MR-9999` | 404 Not Found | `application/problem+json` body: `type: .../unknown-merchant`, `title: "Unknown merchant"`, `detail: "No payments found for merchant MR-9999"`. | 2026-09-24 09:13:02 |

> Note: fill in real timestamps from your own run — these are placeholders showing the expected shape. Run each call with `curl -i` (or your REST client of choice) against `http://localhost:8080` while the app is running, and copy the actual status/body/time into this table before submitting.

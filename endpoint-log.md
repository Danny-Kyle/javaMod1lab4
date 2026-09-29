# Endpoint Log

Run against the app started with the Testcontainers-backed `PaymentControllerIT` (same behavior applies when running locally against a real PostgreSQL instance).

| Request | Status | Body summary | Time |
|---|---|---|---|
| `POST /payments` `{"merchantId":"MR-4471","amountMinor":128450,"currency":"GBP"}` | HTTP/1.1 201 Location: /payments/ff7b9400-8cbd-4061-80c5-162007a457b3 Content-Type: application/json Transfer-Encoding: chunked Date: Mon, 28 Sep 2026 10:11:28 GMT  | Returns the created payment: `id`, `merchantId: "MR-4471"`, `amountMinor: 128450`, `currency: "GBP"`. `Location` header set to `/payments/{id}`. | TIME 2.096414s |
| `POST /payments` `{"merchantId":"MR-4471","amountMinor":-500,"currency":"GBP"}` | HTTP/1.1 400 Content-Type: application/problem+json Transfer-Encoding: chunked Date: Mon, 28 Sep 2026 10:11:40 GMT Connection: close  | {"detail":"Invalid request content.","instance":"/payments","status":400,"title":"Bad Request"} | TIME 0.193873s |
| `POST /payments` `{"merchantId":"","amountMinor":1000,"currency":"GBP"}` | HTTP/1.1 400 Content-Type: application/problem+json Transfer-Encoding: chunked Date: Mon, 28 Sep 2026 10:11:46 GMT Connection: close  | {"detail":"Invalid request content.","instance":"/payments","status":400,"title":"Bad Request"} | TIME 0.011288s |
| `GET /payments/settlement?merchantId=MR-4471` | HTTP/1.1 200  Content-Type: application/json Content-Length: 49 Date: Mon, 28 Sep 2026 10:11:55 GMT | {"merchantId":"MR-4471","amountOwedMinor":124469} | TIME 1.288897s|
| `GET /payments/settlement?merchantId=MR-9999` | HTTP/1.1 404 Content-Type: application/problem+jsonTransfer-Encoding: chunked Date: Mon, 28 Sep 2026 10:12:01 GMT | {"detail":"No payments found for merchant MR-9999","instance":"/payments/settlement","status":404,"title":"Unknown merchant","type":"https://ledger.example.com/problems/unknown-merchant"} | TIME 0.028719s |


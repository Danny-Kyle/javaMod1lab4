package com.example.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses the plain JDK HttpClient plus a tiny hand-rolled JSON field extractor
 * instead of a REST test client / JSON-mapping library. Spring Boot 4 moved
 * TestRestTemplate into a separate module, stopped auto-configuring it on
 * @SpringBootTest, and no longer guarantees Jackson is on the classpath just
 * because spring-boot-starter-web is present (it was split into its own
 * starter). Since we only ever need to read two numeric fields back out of
 * our own, fully-controlled response JSON, a two-line regex avoids chasing
 * any more of that module churn.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void postedPaymentIsReflectedInSettlementNetOfFee() throws Exception {
        String baseUrl = "http://localhost:" + port;

        String requestBody = """
                {"merchantId":"MR-4471","amountMinor":128450,"currency":"GBP"}""";

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/payments"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(postResponse.statusCode()).isEqualTo(201);
        assertThat(postResponse.headers().firstValue("Location")).isPresent();
        assertThat(extractLong(postResponse.body(), "amountMinor")).isEqualTo(128_450L);

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/payments/settlement?merchantId=MR-4471"))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(getResponse.statusCode()).isEqualTo(200);
        assertThat(extractLong(getResponse.body(), "amountOwedMinor")).isEqualTo(124_469L);
    }

    private static long extractLong(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Field \"" + field + "\" not found in response body: " + json);
        }
        return Long.parseLong(matcher.group(1));
    }
}

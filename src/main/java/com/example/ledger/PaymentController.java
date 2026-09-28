package com.example.ledger;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final SettlementService settlementService;

    public PaymentController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> recordPayment(@Valid @RequestBody RecordPaymentRequest request) {
        PaymentEntity saved = settlementService.recordPayment(
                request.merchantId(), request.amountMinor(), request.currency());

        PaymentResponse body = new PaymentResponse(
                saved.getId(), saved.getMerchantId(), saved.getAmountMinor(), saved.getCurrency());

        return ResponseEntity.created(URI.create("/payments/" + saved.getId())).body(body);
    }

    @GetMapping("/settlement")
    public SettlementResponse settlement(@RequestParam String merchantId) {
        long owedMinor = settlementService.amountOwed(merchantId);
        return new SettlementResponse(merchantId, owedMinor);
    }

    public record RecordPaymentRequest(
            @NotBlank String merchantId,
            @Positive long amountMinor,
            @NotBlank String currency) {
    }

    public record PaymentResponse(
            String id,
            String merchantId,
            long amountMinor,
            String currency) {
    }

    public record SettlementResponse(
            String merchantId,
            long amountOwedMinor) {
    }
}

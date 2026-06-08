package com.citizens.dti.api;

import com.citizens.dti.model.BehavioralProfile;
import com.citizens.dti.model.IdentityTwin;
import com.citizens.dti.repository.IdentityTwinRepository;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/twins")
public class TwinController {

    private final IdentityTwinRepository repository;

    public TwinController(IdentityTwinRepository repository) {
        this.repository = repository;
    }

    /** Inspect the current learned state of a customer's identity twin. */
    @GetMapping("/{customerId}")
    public ResponseEntity<TwinSnapshot> get(@PathVariable String customerId) {
        return repository.find(customerId)
                .map(twin -> ResponseEntity.ok(toSnapshot(twin)))
                .orElse(ResponseEntity.notFound().build());
    }

    private TwinSnapshot toSnapshot(IdentityTwin twin) {
        BehavioralProfile p = twin.getProfile();
        return new TwinSnapshot(
                twin.getCustomerId(),
                p.getTransactionCount(),
                round(p.amountMean()),
                round(p.amountStdDev()),
                twin.getCreatedAt(),
                twin.getLastUpdated()
        );
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Read-only projection of the twin for the inspection API. */
    public record TwinSnapshot(
            String customerId,
            long transactionCount,
            double meanAmount,
            double stdDevAmount,
            Instant createdAt,
            Instant lastUpdated
    ) {}
}

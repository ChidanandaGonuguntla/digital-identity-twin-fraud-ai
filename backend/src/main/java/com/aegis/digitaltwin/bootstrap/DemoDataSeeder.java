package com.aegis.digitaltwin.bootstrap;

import com.aegis.digitaltwin.domain.EventType;
import com.aegis.digitaltwin.dto.ActivityEventRequest;
import com.aegis.digitaltwin.dto.CreateTwinRequest;
import com.aegis.digitaltwin.repository.CustomerTwinRepository;
import com.aegis.digitaltwin.service.DigitalTwinService;
import com.aegis.digitaltwin.service.FraudDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {
    private final CustomerTwinRepository twinRepository;
    private final DigitalTwinService digitalTwinService;
    private final FraudDecisionService fraudDecisionService;

    @Override
    public void run(String... args) {
        if (twinRepository.count() > 0) return;

        digitalTwinService.createTwin(new CreateTwinRequest("CUST1001", "Ava Thompson", "ava@example.com", "+1-704-555-0101", "Charlotte", "US", "RETAIL_AFFLUENT", List.of("iphone-15", "macbook-pro"), List.of("Charlotte", "New York"), List.of("Amazon", "Target", "Walmart"), List.of("Duke Energy", "Verizon"), new BigDecimal("125.50")));
        digitalTwinService.createTwin(new CreateTwinRequest("CUST1002", "Noah Williams", "noah@example.com", "+1-704-555-0102", "Raleigh", "US", "RETAIL", List.of("pixel-8"), List.of("Raleigh", "Charlotte"), List.of("Costco", "Netflix", "Uber"), List.of("Spectrum", "Bank Loan"), new BigDecimal("210.00")));
        digitalTwinService.createTwin(new CreateTwinRequest("CUST1003", "Mia Patel", "mia@example.com", "+1-704-555-0103", "Atlanta", "US", "SMALL_BUSINESS", List.of("iphone-14", "windows-laptop"), List.of("Atlanta", "Charlotte"), List.of("Home Depot", "Delta", "Staples"), List.of("Vendor-ACME", "Payroll"), new BigDecimal("780.00")));
        digitalTwinService.createTwin(new CreateTwinRequest("CUST1004", "Liam Chen", "liam@example.com", "+1-704-555-0104", "Austin", "US", "RETAIL", List.of("samsung-s24"), List.of("Austin", "Dallas"), List.of("Target", "Apple", "Starbucks"), List.of("Rent Portal", "Electric Co"), new BigDecimal("92.00")));

        fraudDecisionService.evaluate(new ActivityEventRequest("CUST1001", EventType.PAYMENT, new BigDecimal("84.75"), "Amazon", null, "iphone-15", "Charlotte", "10.1.10.8", 9, "RETAIL"));
        fraudDecisionService.evaluate(new ActivityEventRequest("CUST1001", EventType.PAYMENT, new BigDecimal("4500.00"), "Unknown Crypto Exchange", "WALLET-8841", "android-new-919", "Lagos", "102.88.10.44", 2, "CRYPTO"));
        fraudDecisionService.evaluate(new ActivityEventRequest("CUST1002", EventType.TRANSFER, new BigDecimal("950.00"), null, "PAYEE_NEW_WIRE_72", "pixel-8", "Raleigh", "10.1.20.7", 20, "WIRE"));
        fraudDecisionService.evaluate(new ActivityEventRequest("CUST1003", EventType.CASH_OUT, new BigDecimal("6200.00"), "ATM Network", "WALLET-119", "unknown-browser", "Miami", "185.66.22.91", 1, "CASH_OUT"));
        fraudDecisionService.evaluate(new ActivityEventRequest("CUST1004", EventType.PAYMENT, new BigDecimal("48.20"), "Target", null, "samsung-s24", "Austin", "10.1.30.2", 18, "RETAIL"));
    }
}

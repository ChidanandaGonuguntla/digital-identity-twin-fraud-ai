package com.aegis.digitaltwin.controller;

import com.aegis.digitaltwin.dto.DashboardResponse;
import com.aegis.digitaltwin.entity.CustomerEvent;
import com.aegis.digitaltwin.repository.CustomerEventRepository;
import com.aegis.digitaltwin.service.DashboardService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DashboardController {
  private final DashboardService dashboardService;
  private final CustomerEventRepository eventRepository;

  @GetMapping("/dashboard")
  public DashboardResponse dashboard() {
    return dashboardService.getDashboard();
  }

  @GetMapping("/events/recent")
  public List<CustomerEvent> recent() {
    return eventRepository.findTop20ByOrderByCreatedAtDesc();
  }

  @GetMapping("/dataset/sample")
  public List<Map<String, Object>> sampleDataset() {
    return List.of(
        Map.of(
            "step",
            1,
            "type",
            "PAYMENT",
            "amount",
            186.40,
            "nameOrig",
            "CUST1001",
            "nameDest",
            "MERCHANT_AMAZON",
            "isFraud",
            0),
        Map.of(
            "step",
            2,
            "type",
            "TRANSFER",
            "amount",
            8700.00,
            "nameOrig",
            "CUST1002",
            "nameDest",
            "PAYEE_NEW_WIRE_72",
            "isFraud",
            1),
        Map.of(
            "step",
            3,
            "type",
            "CASH_OUT",
            "amount",
            4200.00,
            "nameOrig",
            "CUST1003",
            "nameDest",
            "WALLET_981",
            "isFraud",
            1),
        Map.of(
            "step",
            4,
            "type",
            "PAYMENT",
            "amount",
            62.22,
            "nameOrig",
            "CUST1004",
            "nameDest",
            "MERCHANT_TARGET",
            "isFraud",
            0));
  }
}

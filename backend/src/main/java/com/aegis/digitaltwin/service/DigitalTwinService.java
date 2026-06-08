package com.aegis.digitaltwin.service;

import com.aegis.digitaltwin.domain.RiskLevel;
import com.aegis.digitaltwin.dto.CreateTwinRequest;
import com.aegis.digitaltwin.entity.CustomerIdentity;
import com.aegis.digitaltwin.entity.CustomerTwin;
import com.aegis.digitaltwin.repository.CustomerIdentityRepository;
import com.aegis.digitaltwin.repository.CustomerTwinRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DigitalTwinService {
  private final CustomerIdentityRepository identityRepository;
  private final CustomerTwinRepository twinRepository;

  @Transactional
  public CustomerTwin createTwin(CreateTwinRequest request) {
    if (!identityRepository.existsByCustomerId(request.customerId())) {
      identityRepository.save(
          CustomerIdentity.builder()
              .customerId(request.customerId())
              .fullName(request.fullName())
              .email(request.email())
              .phone(request.phone())
              .kycStatus("VERIFIED")
              .homeCity(request.homeCity())
              .homeCountry(request.homeCountry())
              .build());
    }
    CustomerTwin twin =
        twinRepository.findByCustomerId(request.customerId()).orElseGet(CustomerTwin::new);
    twin.setCustomerId(request.customerId());
    twin.setSegment(request.segment() == null ? "RETAIL" : request.segment());
    twin.setKnownDevices(safe(request.knownDevices()));
    twin.setKnownLocations(safe(request.knownLocations()));
    twin.setTrustedMerchants(safe(request.trustedMerchants()));
    twin.setTrustedPayees(safe(request.trustedPayees()));
    twin.setAverageTransactionAmount(request.averageTransactionAmount());
    twin.setTrustScore(twin.getTrustScore() == null ? 820 : twin.getTrustScore());
    twin.setRiskLevel(twin.getRiskLevel() == null ? RiskLevel.LOW : twin.getRiskLevel());
    twin.setNormalLoginStartHour(6);
    twin.setNormalLoginEndHour(22);
    twin.setRecentFailedAuthCount(0);
    return twinRepository.save(twin);
  }

  public List<CustomerTwin> findAll() {
    return twinRepository.findAll();
  }

  public CustomerTwin getTwin(String customerId) {
    return twinRepository
        .findByCustomerId(customerId)
        .orElseThrow(() -> new IllegalArgumentException("Customer twin not found: " + customerId));
  }

  private List<String> safe(List<String> values) {
    return values == null ? List.of() : values;
  }
}

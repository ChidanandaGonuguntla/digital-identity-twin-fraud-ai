package com.aegis.digitaltwin.entity;

import com.aegis.digitaltwin.domain.RiskLevel;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "customer_twin")
public class CustomerTwin {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false, length = 50)
  private String customerId;

  private String segment;
  private BigDecimal averageTransactionAmount;
  private Integer trustScore;

  @Enumerated(EnumType.STRING)
  private RiskLevel riskLevel;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "twin_known_devices", joinColumns = @JoinColumn(name = "twin_id"))
  @Column(name = "device_id")
  @Builder.Default
  private List<String> knownDevices = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "twin_known_locations", joinColumns = @JoinColumn(name = "twin_id"))
  @Column(name = "location")
  @Builder.Default
  private List<String> knownLocations = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "twin_trusted_merchants", joinColumns = @JoinColumn(name = "twin_id"))
  @Column(name = "merchant")
  @Builder.Default
  private List<String> trustedMerchants = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "twin_trusted_payees", joinColumns = @JoinColumn(name = "twin_id"))
  @Column(name = "payee")
  @Builder.Default
  private List<String> trustedPayees = new ArrayList<>();

  private Integer normalLoginStartHour;
  private Integer normalLoginEndHour;
  private Integer recentFailedAuthCount;
  private Instant lastHighRiskEventAt;
  private Instant createdAt;
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}

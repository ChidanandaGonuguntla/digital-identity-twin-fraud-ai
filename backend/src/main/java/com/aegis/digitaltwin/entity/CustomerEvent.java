package com.aegis.digitaltwin.entity;

import com.aegis.digitaltwin.domain.DecisionType;
import com.aegis.digitaltwin.domain.EventType;
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
@Table(name = "customer_event")
public class CustomerEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false, length = 80)
  private String eventId;

  @Column(nullable = false, length = 50)
  private String customerId;

  @Enumerated(EnumType.STRING)
  private EventType eventType;

  private BigDecimal amount;
  private String merchant;
  private String payee;
  private String deviceId;
  private String location;
  private String ipAddress;
  private Integer loginHour;
  private String merchantCategory;
  private Integer riskScore;

  @Enumerated(EnumType.STRING)
  private DecisionType decision;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "event_risk_reasons", joinColumns = @JoinColumn(name = "event_id"))
  @Column(name = "reason", length = 500)
  @Builder.Default
  private List<String> reasons = new ArrayList<>();

  private Instant createdAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }
}

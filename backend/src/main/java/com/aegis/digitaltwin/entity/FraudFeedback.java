package com.aegis.digitaltwin.entity;

import com.aegis.digitaltwin.domain.FeedbackOutcome;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "fraud_feedback")
public class FraudFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventId;
    private String customerId;

    @Enumerated(EnumType.STRING)
    private FeedbackOutcome outcome;

    @Column(length = 2000)
    private String comments;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

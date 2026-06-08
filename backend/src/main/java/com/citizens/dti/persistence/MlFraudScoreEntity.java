package com.citizens.dti.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA mapping for {@code identity_twin.ml_fraud_score} — the per-transaction decision
 * record. The deterministic engine currently fills {@code ruleScore} and
 * {@code finalRiskScore}; the ML probability/score columns are populated once the ONNX
 * Fraud Model Service and Risk Fusion Engine are wired in.
 */
@Entity
@Table(name = "ml_fraud_score", schema = "identity_twin")
public class MlFraudScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "rule_score", nullable = false)
    private double ruleScore;

    @Column(name = "ml_fraud_probability")
    private Double mlFraudProbability;

    @Column(name = "ml_score")
    private Double mlScore;

    @Column(name = "rule_weight", nullable = false)
    private double ruleWeight = 1.0;

    @Column(name = "ml_weight", nullable = false)
    private double mlWeight = 0.0;

    @Column(name = "final_risk_score", nullable = false)
    private double finalRiskScore;

    @Column(name = "final_decision", nullable = false)
    private String finalDecision;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "model_version")
    private String modelVersion;

    /**
     * Until the model emits SHAP contributions, this holds the deterministic engine's
     * reason codes. Once the fusion layer lands it holds [{name, contribution}] entries.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_risk_signals", columnDefinition = "jsonb")
    private List<String> topRiskSignals;

    @Column(name = "cold_start", nullable = false)
    private boolean coldStart;

    @Column(name = "inference_latency_ms")
    private Integer inferenceLatencyMs;

    @Column(name = "total_latency_ms")
    private Integer totalLatencyMs;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt = Instant.now();

    public Long getId() { return id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public double getRuleScore() { return ruleScore; }
    public void setRuleScore(double ruleScore) { this.ruleScore = ruleScore; }

    public Double getMlFraudProbability() { return mlFraudProbability; }
    public void setMlFraudProbability(Double v) { this.mlFraudProbability = v; }

    public Double getMlScore() { return mlScore; }
    public void setMlScore(Double mlScore) { this.mlScore = mlScore; }

    public double getRuleWeight() { return ruleWeight; }
    public void setRuleWeight(double ruleWeight) { this.ruleWeight = ruleWeight; }

    public double getMlWeight() { return mlWeight; }
    public void setMlWeight(double mlWeight) { this.mlWeight = mlWeight; }

    public double getFinalRiskScore() { return finalRiskScore; }
    public void setFinalRiskScore(double finalRiskScore) { this.finalRiskScore = finalRiskScore; }

    public String getFinalDecision() { return finalDecision; }
    public void setFinalDecision(String finalDecision) { this.finalDecision = finalDecision; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public List<String> getTopRiskSignals() { return topRiskSignals; }
    public void setTopRiskSignals(List<String> topRiskSignals) { this.topRiskSignals = topRiskSignals; }

    public boolean isColdStart() { return coldStart; }
    public void setColdStart(boolean coldStart) { this.coldStart = coldStart; }

    public Integer getInferenceLatencyMs() { return inferenceLatencyMs; }
    public void setInferenceLatencyMs(Integer v) { this.inferenceLatencyMs = v; }

    public Integer getTotalLatencyMs() { return totalLatencyMs; }
    public void setTotalLatencyMs(Integer v) { this.totalLatencyMs = v; }

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}

package com.citizens.dti.persistence;

import com.citizens.dti.model.Decision;
import com.citizens.dti.model.RiskAssessment;
import com.citizens.dti.model.TransactionEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the raw transaction and the fraud decision record. The deterministic engine's
 * ALLOW/CHALLENGE/BLOCK maps to the schema's APPROVE/REVIEW/DECLINE. ML probability fields stay
 * null until the ONNX Fraud Model Service + Risk Fusion Engine are wired in, at which point
 * final_risk_score becomes the weighted fusion of rule and ML scores.
 */
@Service
public class DecisionRecordService {

  private final TransactionJpaRepository transactionRepository;
  private final MlFraudScoreJpaRepository scoreRepository;

  public DecisionRecordService(
      TransactionJpaRepository transactionRepository, MlFraudScoreJpaRepository scoreRepository) {
    this.transactionRepository = transactionRepository;
    this.scoreRepository = scoreRepository;
  }

  @Transactional
  public void record(TransactionEvent event, RiskAssessment assessment, Integer totalLatencyMs) {
    if (!transactionRepository.existsById(event.transactionId())) {
      transactionRepository.save(toTransaction(event));
    }
    scoreRepository.save(toScore(event, assessment, totalLatencyMs));
  }

  private TransactionEntity toTransaction(TransactionEvent e) {
    TransactionEntity t = new TransactionEntity();
    t.setTransactionId(e.transactionId());
    t.setCustomerId(e.customerId());
    t.setAmount(e.amount());
    t.setMerchantCategory(e.merchantCategory());
    t.setDeviceId(e.deviceId());
    t.setLatitude(e.latitude());
    t.setLongitude(e.longitude());
    t.setEventTime(e.timestamp());
    return t;
  }

  private MlFraudScoreEntity toScore(TransactionEvent e, RiskAssessment a, Integer totalLatencyMs) {
    MlFraudScoreEntity m = new MlFraudScoreEntity();
    m.setTransactionId(a.transactionId());
    m.setCustomerId(a.customerId());
    m.setRuleScore(a.riskScore());
    // Deterministic-only tier for now: 100% rule weight, no ML probability yet.
    m.setRuleWeight(1.0);
    m.setMlWeight(0.0);
    m.setFinalRiskScore(a.riskScore());
    m.setFinalDecision(mapDecision(a.decision()));
    m.setModelName("deterministic-rules");
    m.setModelVersion("v1");
    m.setTopRiskSignals(a.reasons());
    m.setColdStart(a.coldStart());
    m.setTotalLatencyMs(totalLatencyMs);
    return m;
  }

  /** Engine verdict -> schema decision vocabulary. */
  private String mapDecision(Decision decision) {
    return switch (decision) {
      case ALLOW -> "APPROVE";
      case CHALLENGE -> "REVIEW";
      case BLOCK -> "DECLINE";
    };
  }
}

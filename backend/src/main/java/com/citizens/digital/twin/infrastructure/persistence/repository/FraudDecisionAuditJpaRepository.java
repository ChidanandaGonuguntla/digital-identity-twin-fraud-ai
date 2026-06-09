package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.FraudDecisionAuditEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FraudDecisionAuditJpaRepository
    extends JpaRepository<FraudDecisionAuditEntity, String>,
        JpaSpecificationExecutor<FraudDecisionAuditEntity> {
  List<FraudDecisionAuditEntity> findTop50ByOrderByAssessedAtDesc();

  Page<FraudDecisionAuditEntity> findAllByOrderByAssessedAtDesc(Pageable pageable);

  Page<FraudDecisionAuditEntity> findByDecisionOrderByAssessedAtDesc(
      String decision, Pageable pageable);

  Page<FraudDecisionAuditEntity> findByCustomerIdOrderByAssessedAtDesc(
      String customerId, Pageable pageable);

  Page<FraudDecisionAuditEntity> findByTransactionIdOrderByAssessedAtDesc(
      String transactionId, Pageable pageable);

  boolean existsByTransactionId(String transactionId);

  Optional<FraudDecisionAuditEntity> findFirstByTransactionIdOrderByAssessedAtDesc(
      String transactionId);

  @Query(
      """
      SELECT a FROM FraudDecisionAuditEntity a
      WHERE a.decision = 'CHALLENGE'
         OR (a.decision = 'BLOCK' AND a.finalScore >= 20)
      ORDER BY a.assessedAt DESC
      """)
  Page<FraudDecisionAuditEntity> findStepUpQueue(Pageable pageable);

  @Query(
      """
      SELECT COUNT(a),
             COALESCE(SUM(CASE WHEN a.decision = 'BLOCK' THEN 1 ELSE 0 END), 0),
             COALESCE(SUM(CASE WHEN a.decision = 'CHALLENGE' THEN 1 ELSE 0 END), 0),
             COALESCE(SUM(CASE WHEN a.decision = 'ALLOW' THEN 1 ELSE 0 END), 0),
             COALESCE(AVG(a.finalScore), 0)
      FROM FraudDecisionAuditEntity a
      """)
  Object[] summaryAggregate();

  @Query(
      value =
          """
          SELECT COALESCE(
            (SELECT percentile_cont(0.95) WITHIN GROUP (ORDER BY latency_ms)
             FROM identity_twin.fraud_decision_audit),
            0)
          """,
      nativeQuery = true)
  Object p95Latency();

  @Query(
      value =
          """
          SELECT COALESCE(SUM((event_snapshot_json->>'amount')::double precision), 0)
          FROM identity_twin.fraud_decision_audit
          WHERE decision = 'BLOCK'
          """,
      nativeQuery = true)
  double preventedAmount();

  @Query(
      value =
          """
          SELECT date_trunc('hour', assessed_at) AS bucket,
                 SUM(CASE WHEN decision = 'ALLOW' THEN 1 ELSE 0 END) AS allow_count,
                 SUM(CASE WHEN decision = 'CHALLENGE' THEN 1 ELSE 0 END) AS challenge_count,
                 SUM(CASE WHEN decision = 'BLOCK' THEN 1 ELSE 0 END) AS block_count
          FROM identity_twin.fraud_decision_audit
          WHERE assessed_at >= :since
          GROUP BY 1
          ORDER BY 1
          """,
      nativeQuery = true)
  List<Object[]> trendBuckets(@Param("since") Instant since);

  @Query(
      value =
          """
          SELECT (FLOOR(final_score / 10) * 10)::int AS bucket_start,
                 COUNT(*) AS bucket_count
          FROM identity_twin.fraud_decision_audit
          GROUP BY 1
          ORDER BY 1
          """,
      nativeQuery = true)
  List<Object[]> scoreBuckets();

  @Query(
      value =
          """
          SELECT code,
                 AVG(score_contribution) AS avg_contribution,
                 COUNT(*) AS signal_count
          FROM (
            SELECT elem->>'code' AS code,
                   COALESCE((elem->>'scoreContribution')::double precision, 0) AS score_contribution
            FROM identity_twin.fraud_decision_audit,
                 LATERAL jsonb_array_elements(
                   CASE
                     WHEN reason_codes_json IS NULL THEN '[]'::jsonb
                     WHEN jsonb_typeof(reason_codes_json) = 'array' THEN reason_codes_json
                     ELSE '[]'::jsonb
                   END
                 ) AS elem
          ) parsed
          WHERE code IS NOT NULL AND code <> ''
          GROUP BY code
          ORDER BY signal_count DESC, avg_contribution DESC
          LIMIT 6
          """,
      nativeQuery = true)
  List<Object[]> reasonLeaderboard();

  @Query(
      value =
          """
          SELECT date_trunc('hour', assessed_at) AS bucket,
                 COUNT(*) AS scored,
                 COALESCE(AVG(final_score), 0) AS avg_risk,
                 COALESCE(STDDEV(final_score), 0) AS risk_spread,
                 COALESCE(AVG(latency_ms), 0) AS avg_latency
          FROM identity_twin.fraud_decision_audit
          WHERE assessed_at >= :since
          GROUP BY 1
          ORDER BY 1
          """,
      nativeQuery = true)
  List<Object[]> modelDriftBuckets(@Param("since") Instant since);

  @Query(
      value =
          """
          SELECT
            (SELECT COUNT(*) FROM identity_twin.fraud_decision_audit WHERE assessed_at >= :since1h),
            (SELECT COUNT(*) FROM identity_twin.fraud_decision_audit WHERE assessed_at >= :since24h),
            (SELECT MAX(assessed_at) FROM identity_twin.fraud_decision_audit),
            (SELECT COALESCE(AVG(latency_ms), 0) FROM identity_twin.fraud_decision_audit WHERE assessed_at >= :since1h),
            (SELECT COALESCE(AVG(final_score), 0) FROM identity_twin.fraud_decision_audit WHERE assessed_at >= :since1h),
            (SELECT COALESCE(AVG((score_breakdown_json->>'mlScore')::double precision), 0)
             FROM identity_twin.fraud_decision_audit WHERE assessed_at >= :since1h
               AND score_breakdown_json IS NOT NULL AND score_breakdown_json <> '{}'::jsonb),
            (SELECT COALESCE(AVG((score_breakdown_json->>'ruleScore')::double precision), 0)
             FROM identity_twin.fraud_decision_audit WHERE assessed_at >= :since1h
               AND score_breakdown_json IS NOT NULL AND score_breakdown_json <> '{}'::jsonb),
            (SELECT COALESCE(AVG((score_breakdown_json->>'twinDeviationScore')::double precision), 0)
             FROM identity_twin.fraud_decision_audit WHERE assessed_at >= :since1h
               AND score_breakdown_json IS NOT NULL AND score_breakdown_json <> '{}'::jsonb),
            (SELECT COALESCE(100.0 * SUM(CASE WHEN decision = 'BLOCK' THEN 1 ELSE 0 END)::double precision
             / NULLIF(COUNT(*), 0), 0)
             FROM identity_twin.fraud_decision_audit WHERE assessed_at >= :since1h),
            (SELECT COALESCE(AVG(final_score), 0) FROM identity_twin.fraud_decision_audit),
            (SELECT COALESCE(STDDEV(final_score), 0) FROM identity_twin.fraud_decision_audit WHERE assessed_at >= :since1h)
          """,
      nativeQuery = true)
  Object[] modelLiveMetrics(@Param("since1h") Instant since1h, @Param("since24h") Instant since24h);

  @Query(
      value =
          """
          SELECT
            COALESCE(SUM(CASE WHEN pred AND gt THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN pred AND NOT gt THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN NOT pred AND gt THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN NOT pred AND NOT gt THEN 1 ELSE 0 END), 0),
            COUNT(*)
          FROM (
            SELECT
              (UPPER(a.decision) IN ('BLOCK', 'CHALLENGE')) AS pred,
              (
                UPPER(COALESCE(a.event_snapshot_json #>> '{metadata,expectedDecision}', '')) IN ('BLOCK', 'CHALLENGE')
                OR UPPER(COALESCE(a.event_snapshot_json #>> '{metadata,riskLabel}', '')) IN ('HIGH', 'MEDIUM')
              ) AS gt
            FROM identity_twin.fraud_decision_audit a
            WHERE a.event_snapshot_json IS NOT NULL
              AND a.event_snapshot_json <> '{}'::jsonb
              AND (
                a.event_snapshot_json #>> '{metadata,expectedDecision}' IS NOT NULL
                OR a.event_snapshot_json #>> '{metadata,riskLabel}' IS NOT NULL
              )
          ) labeled
          """,
      nativeQuery = true)
  Object[] modelQualityConfusion();

  @Query(
      value =
          """
          SELECT final_score,
                 CASE
                   WHEN UPPER(COALESCE(event_snapshot_json #>> '{metadata,expectedDecision}', '')) IN ('BLOCK', 'CHALLENGE')
                     OR UPPER(COALESCE(event_snapshot_json #>> '{metadata,riskLabel}', '')) IN ('HIGH', 'MEDIUM')
                   THEN 1 ELSE 0
                 END AS fraud_label
          FROM identity_twin.fraud_decision_audit
          WHERE event_snapshot_json IS NOT NULL
            AND event_snapshot_json <> '{}'::jsonb
            AND (
              event_snapshot_json #>> '{metadata,expectedDecision}' IS NOT NULL
              OR event_snapshot_json #>> '{metadata,riskLabel}' IS NOT NULL
            )
          ORDER BY random()
          LIMIT 25000
          """,
      nativeQuery = true)
  List<Object[]> modelQualityAucSample();

  @Query(
      value =
          """
          SELECT feature_vector_json::text
          FROM identity_twin.fraud_decision_audit
          WHERE feature_vector_json IS NOT NULL
            AND jsonb_typeof(feature_vector_json) = 'object'
            AND assessed_at >= :since
          ORDER BY assessed_at DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<String> recentFeatureVectors(@Param("since") Instant since, @Param("limit") int limit);

  @Query(
      value =
          """
          SELECT feature_vector_json::text
          FROM identity_twin.fraud_decision_audit
          WHERE feature_vector_json IS NOT NULL
            AND jsonb_typeof(feature_vector_json) = 'object'
          ORDER BY assessed_at ASC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<String> baselineFeatureVectors(@Param("limit") int limit);

  @Query(
      value =
          """
          SELECT COALESCE(NULLIF(merchant_category, ''), 'UNKNOWN') AS segment,
                 COUNT(*) AS samples,
                 AVG(CASE WHEN decision = 'BLOCK' THEN 100.0 ELSE 0.0 END) AS block_rate,
                 AVG(CASE WHEN decision = 'BLOCK' AND UPPER(COALESCE(event_snapshot_json #>> '{metadata,riskLabel}', '')) = 'LOW' THEN 100.0 ELSE 0.0 END) AS false_positive_rate
          FROM identity_twin.fraud_decision_audit
          WHERE merchant_category IS NOT NULL
            AND merchant_category <> ''
          GROUP BY 1
          ORDER BY samples DESC
          LIMIT 8
          """,
      nativeQuery = true)
  List<Object[]> biasSegmentMetrics();

  long countByCustomerIdAndAssessedAtAfter(String customerId, Instant since);

  @Query(
      value =
          """
          SELECT COALESCE(SUM(amount), 0)
          FROM identity_twin.fraud_decision_audit
          WHERE customer_id = :customerId AND assessed_at >= :since
          """,
      nativeQuery = true)
  double sumAmountByCustomerIdSince(
      @Param("customerId") String customerId, @Param("since") Instant since);

  @Query(
      value =
          """
          SELECT COUNT(*)
          FROM identity_twin.fraud_decision_audit
          WHERE customer_id = :customerId
            AND assessed_at >= :since
            AND decision IN ('BLOCK', 'CHALLENGE')
          """,
      nativeQuery = true)
  long countBlockedOrChallengedSince(
      @Param("customerId") String customerId, @Param("since") Instant since);

  @Query(
      value =
          """
          SELECT
            COUNT(*) AS scored,
            COALESCE(AVG(champion_score), 0) AS avg_champion,
            COALESCE(AVG(challenger_score), 0) AS avg_challenger,
            COALESCE(AVG(score_delta), 0) AS avg_delta,
            COALESCE(AVG(CASE WHEN model_agreement THEN 1.0 ELSE 0.0 END), 0) AS agreement_rate
          FROM identity_twin.fraud_decision_audit
          WHERE challenger_score IS NOT NULL
            AND assessed_at >= :since
          """,
      nativeQuery = true)
  Object[] championChallengerAggregate(@Param("since") Instant since);

  @Query(
      """
      SELECT a FROM FraudDecisionAuditEntity a
      WHERE a.decision IN ('BLOCK', 'CHALLENGE')
        AND NOT EXISTS (
          SELECT 1 FROM FraudCaseEntity c WHERE c.assessmentId = a.assessmentId
        )
      ORDER BY a.assessedAt DESC
      """)
  Page<FraudDecisionAuditEntity> findNeedingCase(Pageable pageable);

  @Query(
      """
      SELECT COUNT(a) FROM FraudDecisionAuditEntity a
      WHERE a.decision IN ('BLOCK', 'CHALLENGE')
        AND NOT EXISTS (
          SELECT 1 FROM FraudCaseEntity c WHERE c.assessmentId = a.assessmentId
        )
      """)
  long countNeedingCase();
}

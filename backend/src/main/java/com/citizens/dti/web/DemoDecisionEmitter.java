package com.citizens.dti.web;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Optional helper for end-to-end demos: with the {@code demo} profile active, this broadcasts
 * synthetic {@link DecisionEvent}s on an interval so the SENTINEL console (run with
 * VITE_USE_MOCK=false) lights up from the real backend without Kafka wired.
 *
 * <p>Run: {@code mvn spring-boot:run -Dspring-boot.run.profiles=demo} Remove this class entirely
 * once your real decision source is connected.
 */
@Component
@Profile("demo")
public class DemoDecisionEmitter {

  private static final String[] CATEGORIES = {
    "grocery", "gas", "restaurant", "retail", "electronics", "jewelry", "gambling", "travel"
  };
  private static final double[][] HOME = {
    {35.2271, -80.8431}, {40.7128, -74.0060}, {37.7749, -122.4194}
  };
  private static final double[][] FAR = {
    {1.3521, 103.8198}, {55.7558, 37.6173}, {19.0760, 72.8777}
  };

  private final DecisionStreamHandler handler;
  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "demo-decision-emitter");
            t.setDaemon(true);
            return t;
          });

  public DemoDecisionEmitter(DecisionStreamHandler handler) {
    this.handler = handler;
  }

  @PostConstruct
  void start() {
    scheduler.scheduleAtFixedRate(() -> handler.broadcast(randomEvent()), 1, 1, TimeUnit.SECONDS);
  }

  @PreDestroy
  void stop() {
    scheduler.shutdownNow();
  }

  private DecisionEvent randomEvent() {
    ThreadLocalRandom r = ThreadLocalRandom.current();
    String customer = "CUST-" + String.format("%05d", 1001 + r.nextInt(40));
    double roll = r.nextDouble();
    boolean fraud = roll > 0.93;
    boolean suspicious = !fraud && roll > 0.82;

    double amount = Math.round((20 + r.nextDouble() * 160) * 100) / 100.0;
    String category = CATEGORIES[r.nextInt(4)];
    double[] loc = HOME[r.nextInt(HOME.length)].clone();
    String device = customer.toLowerCase() + "-home";
    double score = r.nextDouble() * 8;

    if (fraud || suspicious) {
      double[] far = FAR[r.nextInt(FAR.length)];
      loc = new double[] {far[0] + r.nextGaussian() * 0.2, far[1] + r.nextGaussian() * 0.2};
      category = CATEGORIES[4 + r.nextInt(4)];
      device = "dev-unknown-" + r.nextInt(1_000_000);
      amount =
          Math.round(amount * (fraud ? 8 + r.nextDouble() * 30 : 3 + r.nextDouble() * 4) * 100)
              / 100.0;
      score = fraud ? 70 + r.nextDouble() * 30 : 42 + r.nextDouble() * 20;
    }

    String decision = score >= 70 ? "BLOCK" : score >= 40 ? "CHALLENGE" : "ALLOW";
    List<SignalContribution> signals =
        fraud || suspicious
            ? List.of(
                new SignalContribution("geo_velocity", Math.round(score * 0.5 * 10) / 10.0),
                new SignalContribution("new_device", Math.round(score * 0.3 * 10) / 10.0),
                new SignalContribution("amount_anomaly", Math.round(score * 0.2 * 10) / 10.0))
            : List.of();
    List<String> reasons =
        fraud || suspicious
            ? List.of(
                "Impossible travel detected between consecutive transactions",
                "Unrecognized device fingerprint")
            : List.of("Transaction consistent with the customer behavioral twin");

    return new DecisionEvent(
        "TXN-" + Long.toString(System.nanoTime(), 36).toUpperCase(),
        customer,
        amount,
        category,
        device,
        Math.round(loc[0] * 10000) / 10000.0,
        Math.round(loc[1] * 10000) / 10000.0,
        System.currentTimeMillis(),
        Math.round(score * 100) / 100.0,
        decision,
        false,
        signals,
        reasons);
  }
}

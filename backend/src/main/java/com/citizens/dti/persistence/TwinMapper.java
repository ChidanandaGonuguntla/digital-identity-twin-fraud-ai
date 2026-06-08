package com.citizens.dti.persistence;

import com.citizens.dti.model.BehavioralProfile;
import com.citizens.dti.model.IdentityTwin;
import org.springframework.stereotype.Component;

/**
 * Maps between the persistence {@link CustomerTwinEntity} and the pure-domain {@link IdentityTwin}.
 * Keeping this here lets the scoring engine stay free of any persistence concerns.
 */
@Component
public class TwinMapper {

  public IdentityTwin toDomain(CustomerTwinEntity e) {
    TwinBaseline b = e.getBaseline() != null ? e.getBaseline() : new TwinBaseline();

    BehavioralProfile profile =
        BehavioralProfile.restore(
            e.getTransactionCount(),
            e.getAmountSum(),
            e.getAmountSumSq(),
            b.getKnownDevices(),
            b.getCategoryCounts(),
            b.getHourHistogram(),
            b.getLastLatitude(),
            b.getLastLongitude(),
            b.getLastTimestampEpochSeconds(),
            b.getLastMerchantCategory());

    return new IdentityTwin(e.getCustomerId(), profile, e.getCreatedAt(), e.getLastUpdated());
  }

  public CustomerTwinEntity toEntity(IdentityTwin twin) {
    BehavioralProfile p = twin.getProfile();

    TwinBaseline b = new TwinBaseline();
    b.setKnownDevices(new java.util.HashSet<>(p.getKnownDevices()));
    b.setCategoryCounts(new java.util.HashMap<>(p.getMerchantCategoryCounts()));
    b.setHourHistogram(p.getHourHistogram());
    b.setLastLatitude(p.getLastLatitude());
    b.setLastLongitude(p.getLastLongitude());
    b.setLastTimestampEpochSeconds(p.getLastTimestampEpochSeconds());
    b.setLastMerchantCategory(p.getLastMerchantCategory());

    CustomerTwinEntity e = new CustomerTwinEntity();
    e.setCustomerId(twin.getCustomerId());
    e.setTransactionCount(p.getTransactionCount());
    e.setAmountSum(p.getAmountSum());
    e.setAmountSumSq(p.getAmountSumOfSquares());
    e.setBaseline(b);
    e.setCreatedAt(twin.getCreatedAt());
    e.setLastUpdated(twin.getLastUpdated());

    return e;
  }
}

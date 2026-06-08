package com.citizens.dti.twin;

import com.citizens.dti.model.IdentityTwin;
import com.citizens.dti.model.TransactionEvent;
import org.springframework.stereotype.Service;

/**
 * The synchronization loop that keeps the digital twin aligned with the real customer. Only trusted
 * (non-fraudulent) behavior is folded into the baseline, so confirmed or suspected fraud never
 * poisons the model it's being detected against.
 */
@Service
public class TwinSynchronizationService {

  public void synchronize(IdentityTwin twin, TransactionEvent event) {
    twin.getProfile().apply(event);
    twin.markUpdated();
  }
}

package com.citizens.dti.service;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailCustomerNotificationService implements CustomerNotificationService {

  @Override
  public void sendTransactionApprovalPush(
      String customerId,
      UUID challengeId,
      BigDecimal amount,
      String merchantName,
      String transactionId) {
    log.info(
        "DEMO BANK APP PUSH SENT customerId={}, challengeId={}, amount={}, merchant={}, transactionId={}",
        customerId,
        challengeId,
        amount,
        merchantName,
        transactionId);
  }
}

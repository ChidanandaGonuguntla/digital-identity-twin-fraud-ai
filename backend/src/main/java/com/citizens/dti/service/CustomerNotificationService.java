package com.citizens.dti.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface CustomerNotificationService {

  void sendTransactionApprovalPush(
      String customerId,
      UUID challengeId,
      BigDecimal amount,
      String merchantName,
      String transactionId);
}

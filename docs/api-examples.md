# API Examples

## Evaluate Fraud

```bash
curl -X POST http://localhost:8080/api/v1/fraud/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "customerId":"CUST1001",
    "eventType":"PAYMENT",
    "amount":2500,
    "merchant":"Unknown Crypto Exchange",
    "deviceId":"android-new-999",
    "location":"Lagos",
    "ipAddress":"102.88.10.44"
  }'
```

## Submit Feedback

```bash
curl -X POST http://localhost:8080/api/v1/fraud/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "customerId":"CUST1001",
    "eventId":"EVT10001",
    "outcome":"CONFIRMED_FRAUD",
    "comments":"Customer denied this transaction"
  }'
```

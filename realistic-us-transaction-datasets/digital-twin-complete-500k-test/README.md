# Complete 500k Test Dataset for Digital Identity Twin Fraud ML

This is the held-out validation/test dataset. Do not train on this file.

## File
`data/test_500k_transactions_COMPLETE.jsonl.gz`

## Row count
500,000 rows.

## Verify
```bash
python scripts/verify_dataset.py --file data/test_500k_transactions_COMPLETE.jsonl.gz --expected 500000
```

## Publish to Kafka for validation
```bash
pip install kafka-python

python scripts/publish_jsonl_gz_to_kafka.py \
  --file data/test_500k_transactions_COMPLETE.jsonl.gz \
  --topic dti.transaction-events \
  --bootstrap localhost:9092 \
  --sleep-ms 1
```

## Notice
This is privacy-safe USA-like synthetic data, not real customer banking/card data.

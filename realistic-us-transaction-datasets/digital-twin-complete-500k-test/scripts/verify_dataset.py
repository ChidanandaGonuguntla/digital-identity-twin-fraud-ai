#!/usr/bin/env python3
import argparse
import gzip
import json
import sys
import time
import urllib.error
import urllib.request


def open_jsonl(path):
    if path.endswith(".gz"):
        return gzip.open(path, "rt", encoding="utf-8")
    return open(path, "rt", encoding="utf-8")


def load_labels(path, limit=0, prefix=""):
    labels = {}
    with open_jsonl(path) as handle:
        for line in handle:
            line = line.strip()
            if not line:
                continue
            event = json.loads(line)
            transaction_id = event.get("transactionId")
            if not transaction_id:
                continue
            if prefix and not transaction_id.startswith(prefix):
                continue
            labels[transaction_id] = bool(event.get("isFraud"))
            if limit and len(labels) >= limit:
                break
    return labels


def verify_file(path, expected):
    count = 0
    try:
        with open_jsonl(path) as handle:
            for line in handle:
                json.loads(line)
                count += 1
    except Exception as exc:
        print(f"FAILED: {exc}")
        print(f"rows_read_before_failure={count}")
        sys.exit(1)

    print(f"rows={count}")
    if count != expected:
        print(f"FAILED: expected {expected}, found {count}")
        sys.exit(2)
    print("OK: file is complete and all lines are valid JSON")


def fetch_decision(base_url, transaction_id):
    url = (
        f"{base_url.rstrip('/')}/api/v1/audit/transactions/"
        f"{urllib.request.quote(transaction_id, safe='')}?page=0&size=1"
    )
    try:
        with urllib.request.urlopen(url, timeout=15) as response:
            payload = json.loads(response.read().decode())
    except urllib.error.HTTPError:
        return None
    items = payload.get("items") or []
    if not items:
        return None
    return items[0].get("decision")


def validate_labels(path, base_url, limit, wait_seconds, prefix):
    labels = load_labels(path, limit=limit, prefix=prefix)
    if not labels:
        print("FAILED: no labeled transactions loaded")
        sys.exit(3)

    if wait_seconds > 0:
        print(f"waiting {wait_seconds}s for backend processing...")
        time.sleep(wait_seconds)

    tp = fp = tn = fn = missing = 0
    decisions = {}
    for index, (transaction_id, actual_fraud) in enumerate(labels.items(), start=1):
        decision = fetch_decision(base_url, transaction_id)
        if decision is None:
            missing += 1
            continue
        decisions[decision] = decisions.get(decision, 0) + 1
        predicted_fraud = decision in ("BLOCK", "CHALLENGE")
        if actual_fraud and predicted_fraud:
            tp += 1
        elif actual_fraud and not predicted_fraud:
            fn += 1
        elif not actual_fraud and predicted_fraud:
            fp += 1
        else:
            tn += 1
        if index % 1000 == 0:
            print(f"checked={index} matched={tp + fp + tn + fn} missing={missing}")

    matched = tp + fp + tn + fn
    precision = tp / max(tp + fp, 1)
    recall = tp / max(tp + fn, 1)
    f1 = 2 * precision * recall / max(precision + recall, 1e-9)
    print(f"labels={len(labels)} matched={matched} missing={missing}")
    print(f"decisions={decisions}")
    print(f"tp={tp} fp={fp} tn={tn} fn={fn}")
    print(f"precision={precision:.4f} recall={recall:.4f} f1={f1:.4f}")
    if matched == 0:
        print("FAILED: no audit records matched the selected labels")
        sys.exit(4)
    if missing:
        print(f"WARNING: {missing} transactions not found in audit yet")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", required=True)
    parser.add_argument("--expected", type=int, default=500000)
    parser.add_argument("--validate-audit", action="store_true")
    parser.add_argument("--audit-url", default="http://127.0.0.1:9997")
    parser.add_argument("--limit", type=int, default=1000)
    parser.add_argument("--wait-seconds", type=int, default=20)
    parser.add_argument("--transaction-prefix", default="TEST-TXN-")
    args = parser.parse_args()

    if args.validate_audit:
        validate_labels(
            args.file,
            args.audit_url,
            args.limit,
            args.wait_seconds,
            args.transaction_prefix,
        )
        return

    verify_file(args.file, args.expected)


if __name__ == "__main__":
    main()

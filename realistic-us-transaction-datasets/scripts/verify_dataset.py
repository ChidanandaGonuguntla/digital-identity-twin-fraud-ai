#!/usr/bin/env python3
import argparse, gzip, json, sys

parser = argparse.ArgumentParser()
parser.add_argument("--file", required=True)
parser.add_argument("--expected", type=int, default=500000)
args = parser.parse_args()

count = 0
try:
    with gzip.open(args.file, "rt", encoding="utf-8") as f:
        for line in f:
            json.loads(line)
            count += 1
except Exception as e:
    print(f"FAILED: {e}")
    print(f"rows_read_before_failure={count}")
    sys.exit(1)

print(f"rows={count}")
if count != args.expected:
    print(f"FAILED: expected {args.expected}, found {count}")
    sys.exit(2)
print("OK: gzip is complete and all lines are valid JSON")

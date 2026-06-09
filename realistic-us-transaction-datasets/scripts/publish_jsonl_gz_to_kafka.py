#!/usr/bin/env python3
import argparse, gzip, json, time
from kafka import KafkaProducer

parser = argparse.ArgumentParser()
parser.add_argument("--file", required=True)
parser.add_argument("--topic", default="dti.transaction-events")
parser.add_argument("--bootstrap", default="localhost:9092")
parser.add_argument("--sleep-ms", type=float, default=0)
parser.add_argument("--limit", type=int, default=0)
parser.add_argument("--offset", type=int, default=0)
args = parser.parse_args()

producer = KafkaProducer(
    bootstrap_servers=args.bootstrap,
    key_serializer=lambda k: k.encode("utf-8"),
    value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    linger_ms=10,
    batch_size=32768,
    retries=5,
)

count = 0
published = 0
started = time.time()
source = args.file
handle = (
    gzip.open(source, "rt", encoding="utf-8")
    if source.endswith(".gz")
    else open(source, "rt", encoding="utf-8")
)
with handle as f:
    for line in f:
        count += 1
        if count <= args.offset:
            continue
        event = json.loads(line)
        producer.send(args.topic, key=event["customerId"], value=event)
        published += 1
        if published % 10000 == 0:
            producer.flush()
            elapsed = max(time.time() - started, 0.001)
            print(f"published={published} rate={published/elapsed:.0f}/sec")
        if args.sleep_ms:
            time.sleep(args.sleep_ms / 1000.0)
        if args.limit and published >= args.limit:
            break

producer.flush()
producer.close()
print(f"done published={published} skipped={min(count, args.offset)}")

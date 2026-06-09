import argparse
import gzip
import json
import math
import numpy as np
import onnx
from datetime import datetime, timezone
from onnxmltools.convert import convert_xgboost
from onnxmltools.convert.common.data_types import FloatTensorType
from pathlib import Path
from sklearn.metrics import f1_score, precision_score, recall_score, roc_auc_score
from xgboost import XGBClassifier

FEATURE_ORDER = [
    "amount_log",
    "amount_zscore",
    "hour_of_day_norm",
    "is_new_device",
    "is_new_country",
    "is_high_risk_channel",
    "merchant_risk_score",
    "category_frequency",
    "twin_txn_count_norm",
    "geo_distance_norm",
]

HIGH_RISK_CHANNELS = {"TOR", "PROXY", "UNKNOWN"}
HIGH_RISK_CATEGORIES = {
    "electronics",
    "jewelry",
    "jewellery",
    "crypto",
    "gift_card",
    "gift card",
    "travel",
}
EARTH_RADIUS_KM = 6371.0
DATASET_ROOT = Path(__file__).resolve().parents[2] / "realistic-us-transaction-datasets"
TRAIN_DEFAULT = DATASET_ROOT / "data" / "train_500k_transactions_COMPLETE.jsonl.gz"
TEST_DEFAULT = (
        DATASET_ROOT
        / "digital-twin-complete-500k-test"
        / "data"
        / "test_500k_transactions_COMPLETE.jsonl.gz"
)
ARTIFACTS_DIR = Path(__file__).resolve().parent / "artifacts"
MODELS_DIR = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "models"


class CustomerTwin:
    def __init__(self):
        self.transaction_count = 0
        self.amount_sum = 0.0
        self.amount_sum_sq = 0.0
        self.known_devices = set()
        self.usual_countries = set()
        self.category_counts = {}
        self.last_latitude = None
        self.last_longitude = None

    def amount_mean(self):
        if self.transaction_count == 0:
            return 0.0
        return self.amount_sum / self.transaction_count

    def amount_std(self):
        if self.transaction_count < 2:
            return 0.0
        mean = self.amount_mean()
        variance = (self.amount_sum_sq / self.transaction_count) - (mean * mean)
        return math.sqrt(variance) if variance > 0 else 0.0

    def is_known_device(self, device_id):
        return bool(device_id) and device_id.strip().lower() in self.known_devices

    def is_usual_country(self, country_code):
        return bool(country_code) and country_code.strip().upper() in self.usual_countries

    def category_frequency(self, category):
        if self.transaction_count == 0 or not category:
            return 0.0
        key = category.strip().lower()
        return self.category_counts.get(key, 0) / self.transaction_count

    def apply(self, event):
        self.transaction_count += 1
        amount = float(event["amount"])
        self.amount_sum += amount
        self.amount_sum_sq += amount * amount
        device_id = event.get("deviceId")
        if device_id:
            self.known_devices.add(device_id.strip().lower())
        country_code = event.get("countryCode")
        if country_code:
            self.usual_countries.add(country_code.strip().upper())
        category = event.get("merchantCategory")
        if category:
            key = category.strip().lower()
            self.category_counts[key] = self.category_counts.get(key, 0) + 1
        latitude = event.get("latitude")
        longitude = event.get("longitude")
        if latitude is not None and longitude is not None:
            self.last_latitude = float(latitude)
            self.last_longitude = float(longitude)


def clamp(value, low, high):
    return max(low, min(high, value))


def haversine_km(lat1, lon1, lat2, lon2):
    lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    a = math.sin(dlat / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
    return EARTH_RADIUS_KM * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def hour_of_day(timestamp):
    return datetime.fromisoformat(timestamp.replace("Z", "+00:00")).hour


def build_features(twin, event):
    amount = max(float(event["amount"]), 0.01)
    mean = twin.amount_mean()
    std = twin.amount_std()
    hour = hour_of_day(event["timestamp"])
    device_id = event.get("deviceId")
    country_code = event.get("countryCode")
    category = event.get("merchantCategory")
    channel = event.get("channel")
    latitude = event.get("latitude")
    longitude = event.get("longitude")

    is_new_device = (
        1.0
        if device_id
           and twin.transaction_count > 0
           and not twin.is_known_device(device_id)
        else 0.0
    )
    is_new_country = (
        1.0
        if twin.usual_countries
           and country_code
           and not twin.is_usual_country(country_code)
        else 0.0
    )
    is_high_risk_channel = (
        1.0
        if channel and channel.strip().upper() in HIGH_RISK_CHANNELS
        else 0.0
    )
    merchant_risk_score = (
        0.9
        if category and category.strip().lower() in HIGH_RISK_CATEGORIES
        else 0.2
    )
    geo_distance_norm = 0.0
    if (
            latitude is not None
            and longitude is not None
            and twin.last_latitude is not None
            and twin.last_longitude is not None
    ):
        distance_km = haversine_km(
            twin.last_latitude,
            twin.last_longitude,
            float(latitude),
            float(longitude),
        )
        geo_distance_norm = clamp(distance_km, 0.0, 12000.0) / 12000.0

    features = {
        "amount_log": math.log1p(amount) / math.log1p(10000.0),
        "amount_zscore": 0.0
        if std <= 0.0
        else clamp((amount - mean) / std, -4.0, 4.0) / 4.0,
        "hour_of_day_norm": hour / 23.0,
        "is_new_device": is_new_device,
        "is_new_country": is_new_country,
        "is_high_risk_channel": is_high_risk_channel,
        "merchant_risk_score": merchant_risk_score,
        "category_frequency": 1.0 - twin.category_frequency(category),
        "twin_txn_count_norm": clamp(float(twin.transaction_count), 0.0, 200.0) / 200.0,
        "geo_distance_norm": geo_distance_norm,
    }
    return np.array([features[name] for name in FEATURE_ORDER], dtype=np.float32)


def open_jsonl(path):
    if str(path).endswith(".gz"):
        return gzip.open(path, "rt", encoding="utf-8")
    return open(path, "rt", encoding="utf-8")


def load_events(path):
    events = []
    truncated = False
    with open_jsonl(path) as handle:
        try:
            for line in handle:
                line = line.strip()
                if line:
                    events.append(json.loads(line))
        except EOFError:
            truncated = True
    events.sort(key=lambda event: event["timestamp"])
    if truncated:
        print(f"Warning: {path} appears truncated; loaded {len(events):,} events")
    return events


def featurize_events(events, twins, collect=True):
    rows = []
    labels = []
    for event in events:
        twin = twins.setdefault(event["customerId"], CustomerTwin())
        if collect:
            rows.append(build_features(twin, event))
            labels.append(1 if event["isFraud"] else 0)
        twin.apply(event)
    return np.asarray(rows, dtype=np.float32), np.asarray(labels, dtype=np.int32)


def export_model(
        version,
        onnx_name,
        model,
        metrics,
        baseline_mean,
        out_dir,
        training_dataset_version,
):
    onnx_path = out_dir / onnx_name
    initial_type = [("float_input", FloatTensorType([None, len(FEATURE_ORDER)]))]
    onnx_model = convert_xgboost(model, initial_types=initial_type, target_opset=12)
    onnx.save_model(onnx_model, str(onnx_path))
    metadata = {
        "modelName": "digital-twin-fraud-risk",
        "modelVersion": version,
        "modelType": "xgboost-onnx",
        "trainingDatasetVersion": training_dataset_version,
        "featureSchemaVersion": "fraud-features-v1.0.0",
        "onnxFile": onnx_name,
        "inputName": "float_input",
        "outputName": "probabilities",
        "featureOrder": FEATURE_ORDER,
        "metrics": metrics,
        "trainedAt": datetime.now(timezone.utc).isoformat(),
        "baselineMeanScore": baseline_mean,
    }
    metadata_path = out_dir / f"{version}.metadata.json"
    metadata_path.write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    return onnx_path, metadata_path


def evaluate(model, x, y, split_name):
    probs = model.predict_proba(x)[:, 1]
    preds = (probs >= 0.5).astype(int)
    tp = int(((preds == 1) & (y == 1)).sum())
    fp = int(((preds == 1) & (y == 0)).sum())
    tn = int(((preds == 0) & (y == 0)).sum())
    fn = int(((preds == 0) & (y == 1)).sum())
    return {
        "split": split_name,
        "rows": int(len(y)),
        "fraudRate": float(y.mean()),
        "auc": float(roc_auc_score(y, probs)),
        "precision": float(precision_score(y, preds, zero_division=0)),
        "recall": float(recall_score(y, preds, zero_division=0)),
        "f1Score": float(f1_score(y, preds, zero_division=0)),
        "falsePositiveRate": float(fp / max(tn + fp, 1)),
        "truePositives": tp,
        "falsePositives": fp,
        "trueNegatives": tn,
        "falseNegatives": fn,
        "baselineMeanScore": float(np.mean(probs)),
    }


def deployment_metrics(test_metrics):
    return {
        "auc": test_metrics["auc"],
        "precision": test_metrics["precision"],
        "recall": test_metrics["recall"],
        "f1Score": test_metrics["f1Score"],
        "falsePositiveRate": test_metrics["falsePositiveRate"],
    }


def train_and_export(
        version,
        onnx_name,
        params,
        x_train,
        x_test,
        y_train,
        y_test,
        out_dir,
        training_dataset_version,
        write_metrics_report=True,
):
    model = XGBClassifier(**params)
    model.fit(
        x_train,
        y_train,
        eval_set=[(x_test, y_test)],
        verbose=False,
    )
    train_metrics = evaluate(model, x_train, y_train, "train")
    test_metrics = evaluate(model, x_test, y_test, "test")
    metrics_report = {
        "modelVersion": version,
        "modelType": "xgboost-onnx",
        "trainingDatasetVersion": training_dataset_version,
        "featureSchemaVersion": "fraud-features-v1.0.0",
        "featureOrder": FEATURE_ORDER,
        "trainedAt": datetime.now(timezone.utc).isoformat(),
        "train": train_metrics,
        "test": test_metrics,
    }
    if write_metrics_report:
        ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
        metrics_path = ARTIFACTS_DIR / "metrics.json"
        metrics_path.write_text(json.dumps(metrics_report, indent=2), encoding="utf-8")
        MODELS_DIR.mkdir(parents=True, exist_ok=True)
        (MODELS_DIR / "training-metrics.json").write_text(
            json.dumps(metrics_report, indent=2),
            encoding="utf-8",
        )
    return export_model(
        version,
        onnx_name,
        model,
        deployment_metrics(test_metrics),
        test_metrics["baselineMeanScore"],
        out_dir,
        training_dataset_version,
    ), metrics_report


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--train", default=str(TRAIN_DEFAULT))
    parser.add_argument("--test", default=str(TEST_DEFAULT))
    args = parser.parse_args()

    out_dir = MODELS_DIR
    out_dir.mkdir(parents=True, exist_ok=True)
    training_dataset_version = "train-500k-complete+test-500k-complete-v1"

    train_path = Path(args.train)
    test_path = Path(args.test)
    if not train_path.exists():
        raise FileNotFoundError(f"Train dataset not found: {train_path}")
    if not test_path.exists():
        raise FileNotFoundError(f"Test dataset not found: {test_path}")

    print(f"Loading train data from {train_path}")
    train_events = load_events(train_path)
    print(f"Loading test data from {test_path}")
    test_events = load_events(test_path)

    twins = {}
    print("Building causal twin features for train split...")
    x_train, y_train = featurize_events(train_events, twins, collect=True)
    print("Building causal twin features for test split...")
    x_test, y_test = featurize_events(test_events, twins, collect=True)

    print(
        f"train={len(x_train):,} fraud_rate={y_train.mean():.4f} "
        f"test={len(x_test):,} fraud_rate={y_test.mean():.4f}"
    )

    scale_pos_weight = float((y_train == 0).sum() / max((y_train == 1).sum(), 1))
    print(f"scale_pos_weight={scale_pos_weight:.2f}")

    primary_path, primary_metrics = train_and_export(
        "fraud-risk-v1.0.0",
        "fraud-risk-v1.0.0.onnx",
        {
            "n_estimators": 180,
            "max_depth": 6,
            "learning_rate": 0.08,
            "subsample": 0.9,
            "colsample_bytree": 0.9,
            "scale_pos_weight": scale_pos_weight,
            "eval_metric": "logloss",
            "random_state": 42,
        },
        x_train,
        x_test,
        y_train,
        y_test,
        out_dir,
        training_dataset_version,
    )

    rollback_path, rollback_metrics = train_and_export(
        "fraud-risk-v0.9.0",
        "fraud-risk-v0.9.0.onnx",
        {
            "n_estimators": 80,
            "max_depth": 4,
            "learning_rate": 0.12,
            "subsample": 0.8,
            "colsample_bytree": 0.8,
            "scale_pos_weight": scale_pos_weight,
            "eval_metric": "logloss",
            "random_state": 7,
        },
        x_train,
        x_test,
        y_train,
        y_test,
        out_dir,
        training_dataset_version,
        write_metrics_report=False,
    )

    print("\n=== Validation metrics (test_500k) ===")
    test = primary_metrics["test"]
    print(f"  AUC:         {test['auc']:.4f}")
    print(f"  Precision:   {test['precision']:.4f}")
    print(f"  Recall:      {test['recall']:.4f}")
    print(f"  F1:          {test['f1Score']:.4f}")
    print(f"  FPR:         {test['falsePositiveRate']:.4f}")
    print(f"\nMetrics written to {ARTIFACTS_DIR / 'metrics.json'}")
    print("Exported ONNX models:")
    print(primary_path)
    print(rollback_path)


if __name__ == "__main__":
    main()

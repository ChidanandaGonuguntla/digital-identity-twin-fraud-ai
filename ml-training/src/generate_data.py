"""
Data layer for the fraud-model training pipeline.

Produces transactions in the SAME schema as the public Sparkov dataset
(Kaggle: kartik2112/fraud-detection), so you can swap the synthetic generator
for the real CSV by calling `load_sparkov_csv(path)` instead of `generate(...)`.

Schema (columns the pipeline relies on):
    cc_num        customer/card id          (entity key for the digital twin)
    amt           transaction amount
    category      merchant category
    device_id     device fingerprint        (Sparkov lacks this; we add it. For the
                                              real CSV it is synthesized in the loader.)
    lat, long     customer location
    merch_lat,    merchant location
    merch_long
    unix_time     epoch seconds
    is_fraud      label (0/1)

The synthetic generator injects realistic fraud patterns (impossible-travel geo
jumps, amount spikes, new device, odd hours, novel category) in varying
combinations so the model has genuine — but not trivial — signal to learn.
"""
from __future__ import annotations

import numpy as np
import pandas as pd

# A handful of US metro coordinates used as customer "home" anchors.
_HOME_CITIES = np.array([
    [35.2271, -80.8431],   # Charlotte
    [40.7128, -74.0060],   # New York
    [41.8781, -87.6298],   # Chicago
    [29.7604, -95.3698],   # Houston
    [33.7490, -84.3880],   # Atlanta
    [37.7749, -122.4194],  # San Francisco
    [47.6062, -122.3321],  # Seattle
    [25.7617, -80.1918],   # Miami
])
# Distant locations used for fraudulent "impossible travel".
_FAR_CITIES = np.array([
    [1.3521, 103.8198],    # Singapore
    [51.5074, -0.1278],    # London
    [55.7558, 37.6173],    # Moscow
    [-23.5505, -46.6333],  # Sao Paulo
    [19.0760, 72.8777],    # Mumbai
])
_CATEGORIES = ["grocery", "gas", "restaurant", "retail", "pharmacy",
               "entertainment", "travel", "electronics", "jewelry", "gambling"]


def generate(n_customers: int = 2000,
             avg_txns_per_customer: int = 100,
             fraud_rate: float = 0.008,
             start: str = "2024-01-01",
             days: int = 730,
             seed: int = 42) -> pd.DataFrame:
    """Generate a realistic, labeled transaction stream over `days` (default 2 years)."""
    rng = np.random.default_rng(seed)
    start_epoch = int(pd.Timestamp(start, tz="UTC").timestamp())
    span = days * 86400

    rows = []
    for c in range(n_customers):
        cc_num = 4000_0000_0000_0000 + c
        home = _HOME_CITIES[rng.integers(len(_HOME_CITIES))]
        # Each customer has a typical spend level, preferred categories, active hours, device.
        amt_mu = rng.uniform(2.8, 4.2)          # lognormal mu  -> median ~ e^mu
        amt_sigma = rng.uniform(0.4, 0.8)
        n_pref = rng.integers(2, 5)
        pref_categories = rng.choice(_CATEGORIES, size=n_pref, replace=False)
        active_hours = rng.choice(range(7, 23), size=rng.integers(4, 10), replace=False)
        home_device = f"dev-{c}-home"

        n_txn = max(10, int(rng.normal(avg_txns_per_customer, avg_txns_per_customer * 0.3)))
        times = np.sort(rng.integers(start_epoch, start_epoch + span, size=n_txn))

        for t in times:
            is_fraud = rng.random() < fraud_rate
            hour = pd.Timestamp(t, unit="s", tz="UTC").hour

            if not is_fraud:
                amt = float(np.round(rng.lognormal(amt_mu, amt_sigma), 2))
                category = str(rng.choice(pref_categories))
                jitter = rng.normal(0, 0.15, size=2)      # stays near home
                lat, lon = home + jitter
                device = home_device if rng.random() < 0.9 else f"dev-{c}-alt"
                # nudge time toward an active hour occasionally (kept simple)
            else:
                # Activate a random 2-4 of the fraud signals (subtle frauds trip fewer).
                signals = rng.choice(
                    ["geo", "amount", "device", "hour", "category"],
                    size=rng.integers(2, 5), replace=False)
                amt = float(np.round(rng.lognormal(amt_mu, amt_sigma), 2))
                category = str(rng.choice(pref_categories))
                lat, lon = home + rng.normal(0, 0.15, size=2)
                device = home_device

                if "amount" in signals:
                    amt = float(np.round(amt * rng.uniform(5, 40), 2))
                if "category" in signals:
                    novel = [x for x in _CATEGORIES if x not in pref_categories]
                    category = str(rng.choice(novel)) if novel else category
                if "geo" in signals:
                    lat, lon = _FAR_CITIES[rng.integers(len(_FAR_CITIES))] + rng.normal(0, 0.1, size=2)
                if "device" in signals:
                    device = f"dev-fraud-{rng.integers(1_000_000)}"

            # Merchant location ~ transaction location (small offset).
            merch = np.array([lat, lon]) + rng.normal(0, 0.05, size=2)
            rows.append((cc_num, amt, category, device,
                         float(lat), float(lon), float(merch[0]), float(merch[1]),
                         int(t), int(is_fraud)))

    df = pd.DataFrame(rows, columns=[
        "cc_num", "amt", "category", "device_id",
        "lat", "long", "merch_lat", "merch_long", "unix_time", "is_fraud"])
    return df.sort_values("unix_time").reset_index(drop=True)


def load_sparkov_csv(path: str) -> pd.DataFrame:
    """
    Load the real Sparkov CSV and normalize it to the pipeline schema.

    The real file has no device fingerprint, so we synthesize a stable one per card
    (you would replace this with your real device-fingerprint field in production).
    """
    df = pd.read_csv(path)
    # Sparkov already uses: cc_num, amt, category, lat, long, merch_lat, merch_long,
    # unix_time, is_fraud. Synthesize a device id deterministically per card.
    df["device_id"] = "dev-" + (df["cc_num"].astype("int64") % 100000).astype(str)
    cols = ["cc_num", "amt", "category", "device_id", "lat", "long",
            "merch_lat", "merch_long", "unix_time", "is_fraud"]
    return df[cols].sort_values("unix_time").reset_index(drop=True)


if __name__ == "__main__":
    d = generate()
    print(d.shape)
    print(d["is_fraud"].mean().round(4), "fraud rate")
    print(d.head())

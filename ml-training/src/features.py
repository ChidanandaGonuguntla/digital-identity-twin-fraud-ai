"""
Twin feature engineering.

Every feature here is CAUSAL: for a given transaction it uses only that customer's
PAST behavior (or the current event's own attributes). This is the single most
important property in fraud modeling — using any future information leaks the label
and produces a model that looks great offline and fails live.

These same features are what the streaming layer (Kafka Streams -> Valkey) maintains
per customer in production, so the offline training features and the online serving
features are computed from one definition.
"""
from __future__ import annotations

import numpy as np
import pandas as pd

EARTH_RADIUS_KM = 6371.0

# The model's input feature columns, in a fixed order (ONNX export depends on this).
FEATURE_COLUMNS = [
    "amt",
    "hour",
    "day_of_week",
    "amt_log",
    "cust_txn_seq",            # how many prior txns this customer has (tenure / cold-start)
    "amt_zscore_personal",     # amount vs customer's own running mean/std
    "amt_to_prior_max",        # amount / customer's prior max amount
    "txn_count_1h",            # velocity: customer's txns in last hour (incl. current)
    "txn_count_24h",           # velocity: customer's txns in last 24h
    "amt_sum_24h",             # velocity: customer's spend in last 24h
    "secs_since_prev",         # time since this customer's previous txn
    "geo_velocity_kmh",        # implied travel speed from previous location
    "is_new_device",           # device never seen before for this customer
    "is_new_category",         # category never seen before for this customer
]


def _haversine_km(lat1, lon1, lat2, lon2):
    lat1, lon1, lat2, lon2 = map(np.radians, [lat1, lon1, lat2, lon2])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    a = np.sin(dlat / 2) ** 2 + np.cos(lat1) * np.cos(lat2) * np.sin(dlon / 2) ** 2
    return EARTH_RADIUS_KM * 2 * np.arcsin(np.sqrt(a))


def build_features(df: pd.DataFrame) -> pd.DataFrame:
    """Return df with FEATURE_COLUMNS added. Input must have the pipeline schema."""
    df = df.sort_values(["cc_num", "unix_time"]).reset_index(drop=True)
    ts = pd.to_datetime(df["unix_time"], unit="s", utc=True)
    df["hour"] = ts.dt.hour
    df["day_of_week"] = ts.dt.dayofweek
    df["amt_log"] = np.log1p(df["amt"])

    g = df.groupby("cc_num", sort=False)

    # --- Personal running statistics (strictly prior: shift(1) excludes current) ---
    prior_mean = g["amt"].apply(lambda s: s.expanding().mean().shift(1)).reset_index(level=0, drop=True)
    prior_std = g["amt"].apply(lambda s: s.expanding().std().shift(1)).reset_index(level=0, drop=True)
    prior_max = g["amt"].apply(lambda s: s.expanding().max().shift(1)).reset_index(level=0, drop=True)
    df["cust_txn_seq"] = g.cumcount()

    prior_std_floored = prior_std.fillna(0.0).clip(lower=1.0)
    df["amt_zscore_personal"] = ((df["amt"] - prior_mean) / prior_std_floored).fillna(0.0)
    df["amt_to_prior_max"] = (df["amt"] / prior_max).replace([np.inf, -np.inf], np.nan).fillna(1.0)

    # --- Time since previous txn & geo-velocity from previous location ---
    prev_time = g["unix_time"].shift(1)
    df["secs_since_prev"] = (df["unix_time"] - prev_time).fillna(1e7)
    prev_lat = g["lat"].shift(1)
    prev_lon = g["long"].shift(1)
    dist_km = _haversine_km(prev_lat, prev_lon, df["lat"], df["long"])
    hours = (df["secs_since_prev"] / 3600.0).clip(lower=1.0 / 60.0)  # floor 1 min
    df["geo_velocity_kmh"] = (dist_km / hours).fillna(0.0).replace([np.inf, -np.inf], 0.0)

    # --- Rolling time-window velocity (per customer, time-indexed) ---
    def _rolling_counts(grp):
        s = grp.set_index(pd.to_datetime(grp["unix_time"], unit="s", utc=True))
        c1h = s["amt"].rolling("1h").count()
        c24 = s["amt"].rolling("24h").count()
        sum24 = s["amt"].rolling("24h").sum()
        out = pd.DataFrame({
            "txn_count_1h": c1h.values,
            "txn_count_24h": c24.values,
            "amt_sum_24h": sum24.values,
        }, index=grp.index)
        return out

    roll = df.groupby("cc_num", sort=False, group_keys=False).apply(_rolling_counts)
    df[["txn_count_1h", "txn_count_24h", "amt_sum_24h"]] = roll[
        ["txn_count_1h", "txn_count_24h", "amt_sum_24h"]]

    # --- Novelty flags (cumulative "seen before for this customer?") ---
    df["is_new_device"] = _first_seen_flag(df, "device_id")
    df["is_new_category"] = _first_seen_flag(df, "category")

    return df


def _first_seen_flag(df: pd.DataFrame, col: str) -> pd.Series:
    """1 if (cc_num, value) pair has not appeared earlier for this customer, else 0."""
    seen_before = df.duplicated(subset=["cc_num", col], keep="first")
    # duplicated=False on the FIRST occurrence -> that's the "new" one -> flag 1.
    return (~seen_before).astype(int)


if __name__ == "__main__":
    import generate_data
    d = generate_data.generate(n_customers=300, seed=1)
    f = build_features(d)
    print(f[FEATURE_COLUMNS].describe().T[["mean", "min", "max"]])
    print("\nNaNs:", f[FEATURE_COLUMNS].isna().sum().sum())

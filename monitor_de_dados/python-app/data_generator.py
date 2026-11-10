"""Gera transações sintéticas (demo). Em produção, troque por leitura do core bancário."""
import numpy as np
import pandas as pd


def generate(n_customers: int = 40, seed: int = 7) -> pd.DataFrame:
    rng = np.random.default_rng(seed)
    now = pd.Timestamp.now(tz="UTC").floor("s")
    rows = []
    for i in range(n_customers):
        cid = f"C{i:04d}"
        profile = rng.choice(["regular", "faixa", "fracionador"], p=[0.6, 0.25, 0.15])
        for k in range(int(rng.integers(8, 25))):
            ts = now - pd.Timedelta(hours=float(rng.uniform(0, 24 * 30)))
            amount = float(np.clip(rng.lognormal(5.2, 0.9), 5, 2900))
            rows.append((cid, profile, f"{cid}-{k}", amount, ts))
        if profile == "faixa":
            for k in range(int(rng.integers(1, 3))):
                ts = now - pd.Timedelta(days=float(rng.uniform(0, 30)))
                rows.append((cid, profile, f"{cid}-B{k}", float(rng.uniform(3000, 10000)), ts))
        elif profile == "fracionador":
            base = now - pd.Timedelta(days=float(rng.uniform(1, 20)))
            for k in range(int(rng.integers(3, 6))):
                ts = base + pd.Timedelta(hours=float(rng.uniform(0, 30)))
                rows.append((cid, profile, f"{cid}-S{k}", float(rng.uniform(4500, 9800)), ts))
    return pd.DataFrame(rows, columns=["customer_id", "profile", "tx_id", "amount", "timestamp"])

"""Orquestra a verificação: envia transações de cada cliente à API Java e consolida o resultado."""
from concurrent.futures import ThreadPoolExecutor

import pandas as pd
import requests


def _payload(customer_id: str, df: pd.DataFrame) -> dict:
    return {
        "customerId": customer_id,
        "transactions": [
            {
                "id": r.tx_id,
                "amount": round(r.amount, 2),
                "currency": "EUR",
                "timestamp": r.timestamp.strftime("%Y-%m-%dT%H:%M:%SZ"),
            }
            for r in df.itertuples()
        ],
    }


def _call(api_url: str, payload: dict) -> dict:
    r = requests.post(f"{api_url}/api/v1/analysis", json=payload, timeout=60)
    r.raise_for_status()
    return r.json()


def check_health(api_url: str) -> bool:
    try:
        return requests.get(f"{api_url}/api/v1/health", timeout=3).ok
    except requests.RequestException:
        return False


def run_checks(api_url: str, tx: pd.DataFrame, workers: int = 8) -> pd.DataFrame:
    groups = list(tx.groupby("customer_id"))
    with ThreadPoolExecutor(max_workers=workers) as pool:
        results = list(pool.map(lambda g: _call(api_url, _payload(g[0], g[1])), groups))
    res = pd.DataFrame(results)
    res["reasons"] = res["reasons"].apply(lambda x: "; ".join(x))
    return res

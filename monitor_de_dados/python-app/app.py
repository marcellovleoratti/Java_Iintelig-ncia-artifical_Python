import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns
import streamlit as st

from data_generator import generate
from orchestrator import check_health, run_checks

BAND_MIN, BAND_MAX = 3000, 10000
sns.set_theme(style="whitegrid", palette="deep")

st.set_page_config(page_title="Monitor 3k–10k EUR", layout="wide")
st.title("Monitor de movimentações entre 3.000 e 10.000 EUR")

with st.sidebar:
    api_url = st.text_input("URL da API Java", "http://localhost:8080")
    n = st.slider("Clientes simulados", 10, 100, 40)
    run = st.button("Executar verificação", type="primary")
    st.caption("API online" if check_health(api_url) else "API offline — inicie o java-api")

if run:
    tx = generate(n)
    try:
        st.session_state["res"] = run_checks(api_url, tx)
        st.session_state["tx"] = tx
    except Exception as e:  # noqa: BLE001
        st.error(f"Falha ao chamar a API: {e}")

if "res" not in st.session_state:
    st.info("Clique em **Executar verificação** na barra lateral.")
    st.stop()

tx, res = st.session_state["tx"], st.session_state["res"]
res = res.merge(tx.drop_duplicates("customer_id")[["customer_id", "profile"]],
                left_on="customerId", right_on="customer_id").drop(columns="customer_id")

c1, c2, c3 = st.columns(3)
c1.metric("Clientes analisados", len(res))
c2.metric("Com movimento na faixa", int(res["movedInBand"].sum()))
c3.metric("Sinalizados para revisão", int(res["flagged"].sum()))

tab_ver, tab_inv = st.tabs(["Verificação (compliance)", "Oportunidade de investimento"])

with tab_ver:
    left, right = st.columns(2)
    with left:
        fig, ax = plt.subplots(figsize=(6, 4))
        sns.histplot(tx, x="amount", bins=40, log_scale=True, ax=ax, color="#4C72B0")
        ax.axvspan(BAND_MIN, BAND_MAX, color="orange", alpha=0.25, label="Faixa 3k–10k")
        ax.set(title="Distribuição dos valores", xlabel="Valor (EUR, escala log)")
        ax.legend()
        st.pyplot(fig)
    with right:
        fig, ax = plt.subplots(figsize=(6, 4))
        sns.histplot(res, x="riskScore", hue="flagged", multiple="stack", bins=10, ax=ax)
        ax.set(title="Score de risco por cliente", xlabel="Score (0–100)")
        st.pyplot(fig)
    st.dataframe(res.sort_values("riskScore", ascending=False)[
        ["customerId", "riskScore", "flagged", "transactionsInBand", "totalInBand",
         "reasons", "aiExplanation", "engine"]], use_container_width=True)

with tab_inv:
    st.caption("Simulação ilustrativa, não é promessa de rentabilidade nem recomendação. "
               "Rentabilidade passada não garante resultados futuros. Clientes sinalizados são "
               "excluídos desta aba para não gerar contato comercial durante uma revisão de compliance.")
    elegiveis = res[(res["movedInBand"]) & (~res["flagged"])].copy()
    if elegiveis.empty:
        st.warning("Nenhum cliente elegível nesta execução.")
        st.stop()

    rate = st.slider("Rentabilidade anual hipotética (%)", 0.0, 12.0, 3.5, 0.5) / 100
    years = st.slider("Horizonte (anos)", 1, 10, 5)
    cust = st.selectbox("Cliente", elegiveis["customerId"])
    capital = float(elegiveis.loc[elegiveis["customerId"] == cust, "totalInBand"].iloc[0])

    anos = list(range(years + 1))
    curve = pd.concat([
        pd.DataFrame({"Ano": anos, "Saldo": [capital] * len(anos), "Cenário": "Parado na conta"}),
        pd.DataFrame({"Ano": anos, "Saldo": [capital * (1 + rate) ** a for a in anos],
                      "Cenário": f"Investido a {rate:.1%} a.a."}),
    ])
    gain = capital * ((1 + rate) ** years - 1)

    left, right = st.columns(2)
    with left:
        fig, ax = plt.subplots(figsize=(6, 4))
        sns.lineplot(curve, x="Ano", y="Saldo", hue="Cenário", marker="o", ax=ax)
        ax.set(title=f"Cliente {cust}: {capital:,.0f} EUR em {years} anos")
        st.pyplot(fig)
        st.metric("Ganho hipotético no horizonte", f"{gain:,.0f} EUR")
    with right:
        top = elegiveis.nlargest(10, "totalInBand")
        fig, ax = plt.subplots(figsize=(6, 4))
        sns.barplot(top, y="customerId", x="totalInBand", color="#55A868", ax=ax)
        ax.set(title="Maior volume movimentado na faixa (elegíveis)", xlabel="EUR", ylabel="")
        st.pyplot(fig)

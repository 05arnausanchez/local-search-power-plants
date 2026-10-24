"""
Resumen de un CSV de experimentos: media y desviación por configuración y boxplots.

Uso:   python analisis/resumen.py resultados/exp1_operadores.csv
Requisitos: python -m pip install pandas matplotlib scipy numpy
"""
import sys
from itertools import combinations
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from scipy import stats

CONFIG = ["alg", "ops", "init", "heur", "alfa", "penal", "k", "lambda", "cli", "nA", "nB", "nC"]
MEDIDAS = ["beneficio", "tiempo_ms", "pasos", "noGarServ", "garServ", "mwPerdidos", "onA", "onB", "onC"]


def main(ruta):
    df = pd.read_csv(ruta, encoding="utf-8-sig")
    if (df["valida"] == 0).any():
        print("ATENCIÓN: hay soluciones NO válidas (columna valida = 0)\n")
    # columnas de configuración que realmente varían en este experimento
    var = [c for c in CONFIG if c in df and df[c].nunique() > 1] or ["alg"]
    df["config"] = df[var].astype(str).agg(" | ".join, axis=1)

    medidas = [m for m in MEDIDAS if m in df and df[m].notna().any()]
    resumen = df.groupby("config")[medidas].agg(["mean", "std"]).round(1)
    pd.set_option("display.width", 250, "display.max_columns", 50)
    print("Configuración =", " | ".join(var), "\n")
    print(resumen.to_string(), "\n")

    # comparación por parejas con las mismas semillas (t de Student pareada y Wilcoxon)
    piv = df.pivot_table(index="seed", columns="config", values="beneficio")
    print("Comparación pareada del beneficio (mismas semillas):")
    for a, b in combinations(piv.columns, 2):
        x, y = piv[a].dropna(), piv[b].dropna()
        comunes = x.index.intersection(y.index)
        if len(comunes) < 3:
            continue
        x, y = x[comunes], y[comunes]
        t = stats.ttest_rel(x, y)
        try:
            w = stats.wilcoxon(x, y).pvalue
        except ValueError:          # todas las diferencias son 0
            w = float("nan")
        gana = (x > y).sum()
        print(f"  {a}  vs  {b}:  dif. media = {(x - y).mean():.1f}  "
              f"p(t) = {t.pvalue:.4f}  p(Wilcoxon) = {w:.4f}  gana el primero {gana}/{len(x)}")

    # experimento 4: líneas de tiempo frente al tamaño, en vez de boxplots
    if "4clientes" in set(df["exp"].astype(str)):
        grafica_tiempos(df, ruta)
        return

    # boxplots de beneficio y tiempo
    fig, ejes = plt.subplots(1, 2, figsize=(max(8, 1.6 * df["config"].nunique()), 5))
    for eje, m in zip(ejes, ["beneficio", "tiempo_ms"]):
        df.boxplot(column=m, by="config", ax=eje, rot=30)
        eje.set_title(m)
        eje.set_xlabel("")
    fig.suptitle(Path(ruta).stem)
    fig.tight_layout()
    salida = Path("analisis") / (Path(ruta).stem + ".png")
    fig.savefig(salida, dpi=150)
    print(f"\nGráfica guardada en {salida}")


def grafica_tiempos(df, ruta):
    """Experimento 4: tiempo medio (± desviación) frente a clientes y frente a centrales."""
    df = df.assign(centrales=df["nA"] + df["nB"] + df["nC"], tiempo_s=df["tiempo_ms"] / 1000)
    series = [("4clientes", "cli", "Número de clientes (40 centrales)"),
              ("4centrales", "centrales", "Número de centrales (1.000 clientes)")]
    fig, ejes = plt.subplots(1, 2, figsize=(11, 4.2))
    for eje, (exp, x, etiqueta) in zip(ejes, series):
        g = (df[df["exp"] == exp].groupby(x)
             .agg(t=("tiempo_s", "mean"), sd=("tiempo_s", "std"), pasos=("pasos", "mean"))
             .sort_index())
        b = np.polyfit(np.log(g.index), np.log(g["t"]), 1)[0]   # exponente: recta en log-log
        eje.errorbar(g.index, g["t"], yerr=g["sd"], marker="o", capsize=4)
        eje.set_xticks(g.index)
        eje.set_xlabel(etiqueta)
        eje.set_ylabel("Tiempo de búsqueda (s)")
        eje.set_title(f"Crecimiento ≈ tamaño^{b:.2f}")
        eje.grid(alpha=0.3)
        print(f"\n{etiqueta}  (exponente del tiempo = {b:.2f})")
        print(g.assign(t_por_paso_ms=1000 * g["t"] / g["pasos"]).round(2).to_string())
    fig.suptitle("Tiempo de Hill Climbing según el tamaño (media ± desviación)")
    fig.tight_layout()
    salida = Path("analisis") / (Path(ruta).stem + ".png")
    fig.savefig(salida, dpi=150)
    print(f"\nGráfica guardada en {salida}")


if __name__ == "__main__":
    main(sys.argv[1])

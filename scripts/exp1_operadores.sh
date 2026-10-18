#!/bin/bash
# Experimento 1: qué conjunto de operadores da mejores resultados.
# Escenario base, Hill Climbing, una inicialización fija, H1 y H2.
source "$(dirname "$0")/comun.sh"
init="greedy"
csv=$(iniciar_csv exp1_operadores.csv)
for ops in mover mover+swap mover+vaciar mover+swap+vaciar; do
    for heur in h1 h2; do
        for s in $SEMILLAS; do
            ejecutar "$csv" exp=1 seed=$s alg=HC init=$init ops=$ops heur=$heur alfa=10
        done
    done
done

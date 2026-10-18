#!/bin/bash
# Experimento adicional: ponderación alfa de las pérdidas en H2 (alfa = 0 equivale a H1).
source "$(dirname "$0")/comun.sh"
ops="mover+swap+vaciar"
csv=$(iniciar_csv extra_alfa.csv)
for init in orden greedy; do
    for alfa in 0 1 10 50 200; do
        for s in $SEMILLAS; do
            ejecutar "$csv" exp=alfa seed=$s alg=HC init=$init ops=$ops heur=h2 alfa=$alfa
        done
    done
done

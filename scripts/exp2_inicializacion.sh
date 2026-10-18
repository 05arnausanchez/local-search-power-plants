#!/bin/bash
# Experimento 2: qué solución inicial da mejores resultados (operadores fijados en el experimento 1).
source "$(dirname "$0")/comun.sh"
ops="mover+swap+vaciar"   # <-- poned aquí el conjunto elegido en el experimento 1
heur="h2"
csv=$(iniciar_csv exp2_inicializacion.csv)
for init in orden aleatoria greedy; do
    for s in $SEMILLAS; do
        ejecutar "$csv" exp=2 seed=$s alg=HC init=$init ops=$ops heur=$heur alfa=10
    done
done

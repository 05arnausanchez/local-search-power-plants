#!/bin/bash
# Experimento 6: duplicar y triplicar las centrales de tipo C.
source "$(dirname "$0")/comun.sh"
ops="mover+swap+vaciar"; init="greedy"; heur="h2"
k=10000; lambda=0.001; iter=200000   # <-- parámetros elegidos en el experimento 3
csv=$(iniciar_csv exp6_centralesC.csv)
for nC in 25 50 75; do
    for s in $SEMILLAS; do
        ejecutar "$csv" exp=6 seed=$s cen=5,10,$nC alg=HC init=$init ops=$ops heur=$heur
        ejecutar "$csv" exp=6 seed=$s cen=5,10,$nC alg=SA init=$init ops=$ops heur=$heur \
            k=$k lambda=$lambda iter=$iter
    done
done

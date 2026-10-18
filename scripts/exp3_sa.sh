#!/bin/bash
# Experimento 3: parámetros del Simulated Annealing. Rejilla de k y lambda con valores extremos;
# después afinad alrededor del mejor punto cambiando las listas.
source "$(dirname "$0")/comun.sh"
ops="mover+swap+vaciar"; init="greedy"; heur="h2"
iter=200000; stiter=100
csv=$(iniciar_csv exp3_sa.csv)
for k in 1000 10000 100000 1000000; do
    for lambda in 0.01 0.001 0.0001; do
        for s in $SEMILLAS; do
            ejecutar "$csv" exp=3 seed=$s alg=SA init=$init ops=$ops heur=$heur alfa=10 \
                iter=$iter stiter=$stiter k=$k lambda=$lambda
        done
    done
done

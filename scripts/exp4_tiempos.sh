#!/bin/bash
# Experimento 4: evolución del tiempo de Hill Climbing con el tamaño del problema.
source "$(dirname "$0")/comun.sh"
ops="mover+swap+vaciar"; init="greedy"; heur="h2"
csv=$(iniciar_csv exp4_tiempos.csv)
# 4a: 40 centrales, clientes de 250 en 250. A partir de ~1500 clientes los garantizados ya no caben
# en las 40 centrales (no existe solución válida), así que la serie llega hasta 1250.
for cli in 250 500 750 1000 1250; do
    for s in $SEMILLAS; do
        ejecutar "$csv" exp=4clientes seed=$s cli=$cli cen=5,10,25 alg=HC init=$init ops=$ops heur=$heur
    done
done
# 4b: 1000 clientes, centrales de 40 en 40 manteniendo la proporción 5:10:25
for m in 1 2 3 4 5; do
    cen="$((5*m)),$((10*m)),$((25*m))"
    for s in $SEMILLAS; do
        ejecutar "$csv" exp=4centrales seed=$s cli=1000 cen=$cen alg=HC init=$init ops=$ops heur=$heur
    done
done

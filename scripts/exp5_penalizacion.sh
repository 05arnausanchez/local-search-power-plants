#!/bin/bash
# Experimento 5: garantizados por penalización en la heurística, partiendo de la solución vacía.
# Con init=vacia el programa permite que los operadores dejen garantizados sin servir.
source "$(dirname "$0")/comun.sh"
ops="mover+swap+vaciar"; heur="h2"
k=10000; lambda=0.001; iter=200000   # <-- parámetros elegidos en el experimento 3
csv=$(iniciar_csv exp5_penalizacion.csv)
for pen in 0 100 1000 5000 10000 20000 50000 100000; do
    for s in $SEMILLAS; do
        ejecutar "$csv" exp=5 seed=$s alg=HC init=vacia ops=$ops heur=$heur penal=$pen
        ejecutar "$csv" exp=5 seed=$s alg=SA init=vacia ops=$ops heur=$heur penal=$pen \
            k=$k lambda=$lambda iter=$iter
    done
done

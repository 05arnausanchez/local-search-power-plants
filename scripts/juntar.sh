#!/bin/bash
# Junta los CSV parciales de un experimento (expN_..._sufijo.csv) en uno solo, con una única cabecera.
# Uso: bash scripts/juntar.sh exp5_penalizacion
set -e
cd "$(dirname "$0")/.."
n="$1"
awk 'FNR==1 && NR!=1 {next} 1' resultados/"$n"_*.csv > resultados/"$n".csv
echo "resultados/$n.csv: $(($(wc -l < resultados/"$n".csv) - 1)) ejecuciones"

#!/bin/bash
# Funciones comunes para los scripts de experimentos. No se ejecuta directamente (se carga con "source").
set -e
cd "$(dirname "${BASH_SOURCE[0]}")/.."
mkdir -p resultados

# Semillas de cada configuración (se pueden cambiar desde fuera: SEMILLAS="1 2 3" bash scripts/exp1_operadores.sh)
SEMILLAS=${SEMILLAS:-$(seq 1 10)}

# Crea el CSV con la cabecera y escribe su ruta. Con SUFIJO=x el fichero es expN_..._x.csv
# (sirve para repartir las semillas de un experimento entre varias personas o terminales).
iniciar_csv() {
    local ruta="resultados/${1%.csv}${SUFIJO:+_$SUFIJO}.csv"
    java -cp "bin:lib/*" energia.Main header=true > "$ruta"
    echo "$ruta"
}

# Ejecuta una búsqueda y añade su línea al CSV. Uso: ejecutar ruta clave=valor clave=valor ...
ejecutar() {
    local ruta="$1"
    shift
    echo "  $*" >&2
    # si una ejecución falla (p. ej. no hay solución válida) se avisa y se sigue con la siguiente
    java -Xmx2g -cp "bin:lib/*" energia.Main "$@" >> "$ruta" || echo "  FALLO: $*" >&2
}

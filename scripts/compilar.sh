#!/bin/bash
# Compila el proyecto y ejecuta el test del ejemplo a mano.
# Uso (desde cualquier carpeta):  bash scripts/compilar.sh
set -e
cd "$(dirname "$0")/.."
rm -rf bin
mkdir bin
javac -encoding UTF-8 -cp "lib/*" -d bin src/energia/*.java test/energia/*.java
echo "¡Compilación correcta! Las clases se encuentran en bin/"

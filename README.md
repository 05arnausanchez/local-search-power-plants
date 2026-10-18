# Práctica de búsqueda local – IA (FIB, 2026/27 1Q)
## Arnau Sanchez Coll, Alex Sanfeliu Dominguez, Adrián San Miguel González

Asignación de clientes a centrales eléctricas con Hill Climbing y Simulated Annealing (AIMA).

## Requisitos

- JDK 17 o superior (`javac -version` para comprobarlo).
- Las librerías `AIMA.jar` y `CentralEnergia.jar` ya están en `lib/`.

## Compilar

Desde la raíz del repositorio.

Mac / Linux:

```bash
bash scripts/compilar.sh
```

Windows (PowerShell):

```powershell
javac -encoding UTF-8 -cp "lib/*" -d bin src\energia\*.java
```

Las clases compiladas quedan en `bin/`. Si no sale ningún error, ha ido bien.

## Ejecutar una búsqueda

Mac / Linux:

```bash
java -Xmx2g -cp "bin:lib/*" energia.Main seed=1 alg=HC init=greedy ops=mover+swap+vaciar heur=h2
```

Windows (PowerShell): igual, pero con `;` en el classpath: `-cp "bin;lib/*"`.

Imprime **una línea CSV** con el resultado. Con `header=true` se imprimen los nombres de las columnas.
Las más útiles: `beneficio_inicial`, `beneficio` (final), `tiempo_ms`, `pasos` y `valida` (1 = solución válida).

## Parámetros (todos opcionales, formato `clave=valor`)

| Parámetro | Valores | Defecto |
|---|---|---|
| `seed` | semilla del escenario | 1 |
| `alg` | `HC` o `SA` | HC |
| `ops` | combinación de `mover`, `swap`, `vaciar` (p. ej. `mover+swap`) | mover |
| `init` | `greedy`, `orden`, `aleatoria`, `vacia` | greedy |
| `heur` | `h1` = −beneficio, `h2` = −beneficio + alfa·MW perdidos | h1 |
| `alfa` | peso de las pérdidas en h2 (€/MW) | 10 |
| `penal` | penalización por MW garantizado sin servir (experimento 5) | 0 |
| `cen` | centrales A,B,C | 5,10,25 |
| `cli` | número de clientes | 1000 |
| `iter`, `stiter`, `k`, `lambda` | parámetros del Simulated Annealing | 100000, 100, 1000, 0.001 |
| `out` | fichero donde guardar la asignación final (una línea por cliente) | – |
| `legible` | `true` imprime un resumen por líneas en vez de la línea CSV | false |

## Ejemplos

Hill Climbing:

```bash
java -Xmx2g -cp "bin:lib/*" energia.Main seed=1 alg=HC init=greedy ops=mover+swap+vaciar heur=h2
```

Simulated Annealing:

```bash
java -Xmx2g -cp "bin:lib/*" energia.Main seed=1 alg=SA init=greedy ops=mover+swap+vaciar heur=h2 k=10000 lambda=0.001 iter=200000
```

Salida legible:

```bash
java -Xmx2g -cp "bin:lib/*" energia.Main seed=1 alg=HC init=greedy ops=mover+swap+vaciar heur=h2 legible=true
```

## Experimentos

Requisitos para el análisis (una vez):

```bash
sudo apt install python3-pandas python3-matplotlib python3-scipy   # Windows: pip install pandas matplotlib scipy
```

Cada script lanza un experimento completo (10 semillas por configuración) y guarda `resultados/expN_*.csv`.
En Windows, los mismos scripts con extensión `.ps1` (`.\scripts\exp1_operadores.ps1`).

| Script | Experimento |
|---|---|
| `exp1_operadores.sh` | 1. Conjuntos de operadores (HC, H1 y H2) |
| `exp2_inicializacion.sh` | 2. Soluciones iniciales (orden, aleatoria, greedy) |
| `exp3_sa.sh` | 3. Parámetros del Simulated Annealing (k, λ) |
| `exp4_tiempos.sh` | 4. Tiempo de HC al aumentar clientes y centrales |
| `exp5_penalizacion.sh` | 5. Garantizados por penalización desde la solución vacía (HC y SA) |
| `exp6_centralesC.sh` | 6. Duplicar y triplicar las centrales C (HC y SA) |
| `extra_alfa.sh` | Extra: peso α de las pérdidas en H2 |

```bash
bash scripts/exp1_operadores.sh
python3 analisis/resumen.py resultados/exp1_operadores.csv
```

`resumen.py` imprime medias y desviación por configuración, compara el beneficio por parejas
(t pareada y Wilcoxon) y guarda un boxplot en `analisis/`.

**Orden:** los experimentos 1, 2 y 3 fijan operadores, heurística, solución inicial y parámetros del SA.
Si cambian, hay que actualizar los valores marcados con `<--` en los scripts siguientes.

**Repartir semillas** (útil para el experimento 5, el más largo):

```bash
SEMILLAS="1 2 3" SUFIJO=adrian bash scripts/exp5_penalizacion.sh   # genera exp5_penalizacion_adrian.csv
bash scripts/juntar.sh exp5_penalizacion                            # une todos los parciales
```

Nota: con 40 centrales, a partir de unos 1.500 clientes los garantizados no caben y no existe
solución válida, por eso el experimento 4 llega hasta 1.250 clientes.

## Estructura

- `src/energia/`: estado y operadores (`EstadoEnergia`), sucesores HC/SA, heurísticas, goal test y `Main`
- `lib/`: AIMA.jar y CentralEnergia.jar
- `scripts/`: compilación y experimentos (`.sh` para Linux, `.ps1` para Windows)
- `analisis/`: `resumen.py`, estadísticas y gráficas de los resultados
- `resultados/`: CSV de los experimentos
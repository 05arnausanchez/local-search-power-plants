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

## Estructura

- `src/energia/`: estado y operadores (`EstadoEnergia`), sucesores HC/SA, heurísticas, goal test y `Main`
- `lib/`: AIMA.jar y CentralEnergia.jar
- `scripts/`: scripts de ayuda
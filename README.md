# Local Search for Power-Plant Client Assignment

**Artificial Intelligence course (FIB-UPC, 2026/27 Q1) — Assignment 1: local search.**
Authors: Arnau Sanchez Coll, Alex Sanfeliu Dominguez, Adrián San Miguel González.

We assign customers to power plants so that the company's profit is maximised, using **Hill Climbing** and
**Simulated Annealing** (Java, AIMA library), and we study experimentally how the design choices affect the result.

## What is in this project
- **Problem model** (`src/energia/EstadoEnergia.java`): a state is an assignment of every customer to a power plant
  (or to none), with capacity, guaranteed-supply and transmission-loss constraints.
- **Operators**: move a customer, swap two customers, and empty a plant (`SucesoresHC.java`, `SucesoresSA.java`).
- **Heuristics**: h1 = −profit, and h2 = −profit + α·(MW lost in transmission) (`HeuristicaBeneficio.java`,
  `HeuristicaPerdidas.java`).
- **Initial solutions**: greedy, ordered, random, empty.
- **Experiments** (`scripts/`, raw data in `resultados/`, plots and summaries in `analisis/`):
  1. operator sets, 2. initial solution, 3. Simulated Annealing parameters, 4. running time vs. problem size,
  5. penalty for unserved guaranteed supply, 6. number of plants of type C, plus an extra study of α.

## Quick start
Requires JDK 17+. The libraries (`AIMA.jar`, `CentralEnergia.jar`) are included in `lib/`.

```bash
bash scripts/compilar.sh
java -Xmx2g -cp "bin:lib/*" energia.Main seed=1 alg=HC init=greedy ops=mover+swap+vaciar heur=h2
```

On Windows use `;` instead of `:` in the classpath. The program prints one CSV line with the result
(`header=true` prints the column names). All parameters and more examples are described in Spanish in
[`LEEME.md`](LEEME.md).

## Repository layout
```text
src/energia/   Java source code (state, operators, heuristics, main)
lib/           AIMA and the course's power-plant library
scripts/       one script per experiment (.sh and .ps1)
resultados/    raw CSV results of every experiment
analisis/      plots (.png) and numerical summaries (.txt) of each experiment
```

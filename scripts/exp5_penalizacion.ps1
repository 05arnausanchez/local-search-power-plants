# Experimento 5: garantizados por penalización en la heurística, partiendo de la solución vacía.
# Con init=vacia el programa permite que los operadores dejen garantizados sin servir.
. "$PSScriptRoot\comun.ps1"
$ops = "mover+swap+vaciar"; $heur = "h2"
$k = 10000; $lambda = 0.001; $iter = 200000   # <-- parámetros elegidos en el experimento 3
$csv = Iniciar-Csv "exp5_penalizacion.csv"
foreach ($pen in 0, 100, 1000, 5000, 10000, 20000, 50000, 100000) {
    foreach ($s in $Semillas) {
        Ejecutar $csv @("exp=5", "seed=$s", "alg=HC", "init=vacia", "ops=$ops", "heur=$heur", "penal=$pen")
        Ejecutar $csv @("exp=5", "seed=$s", "alg=SA", "init=vacia", "ops=$ops", "heur=$heur", "penal=$pen",
                        "k=$k", "lambda=$lambda", "iter=$iter")
    }
}

# Experimento 6: duplicar y triplicar las centrales de tipo C.
. "$PSScriptRoot\comun.ps1"
$ops = "mover+swap+vaciar"; $init = "greedy"; $heur = "h2"
$k = 10000; $lambda = 0.001; $iter = 200000   # <-- parámetros elegidos en el experimento 3
$csv = Iniciar-Csv "exp6_centralesC.csv"
foreach ($nC in 25, 50, 75) {
    foreach ($s in $Semillas) {
        Ejecutar $csv @("exp=6", "seed=$s", "cen=5,10,$nC", "alg=HC", "init=$init", "ops=$ops", "heur=$heur")
        Ejecutar $csv @("exp=6", "seed=$s", "cen=5,10,$nC", "alg=SA", "init=$init", "ops=$ops", "heur=$heur",
                        "k=$k", "lambda=$lambda", "iter=$iter")
    }
}

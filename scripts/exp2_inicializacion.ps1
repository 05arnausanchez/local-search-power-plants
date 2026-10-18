# Experimento 2: qué solución inicial da mejores resultados (operadores fijados en el experimento 1).
. "$PSScriptRoot\comun.ps1"
$ops = "mover+swap+vaciar"   # <-- poned aquí el conjunto elegido en el experimento 1
$heur = "h2"
$csv = Iniciar-Csv "exp2_inicializacion.csv"
foreach ($init in "orden", "aleatoria", "greedy") {
    foreach ($s in $Semillas) {
        Ejecutar $csv @("exp=2", "seed=$s", "alg=HC", "init=$init", "ops=$ops", "heur=$heur", "alfa=10")
    }
}

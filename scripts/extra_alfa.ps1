# Experimento adicional: ponderación alfa de las pérdidas en H2 (alfa = 0 equivale a H1).
. "$PSScriptRoot\comun.ps1"
$ops = "mover+swap+vaciar"
$csv = Iniciar-Csv "extra_alfa.csv"
foreach ($init in "orden", "greedy") {
    foreach ($alfa in 0, 1, 10, 50, 200) {
        foreach ($s in $Semillas) {
            Ejecutar $csv @("exp=alfa", "seed=$s", "alg=HC", "init=$init", "ops=$ops", "heur=h2", "alfa=$alfa")
        }
    }
}

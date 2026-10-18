# Experimento 1: qué conjunto de operadores da mejores resultados.
# Escenario base, Hill Climbing, una inicialización fija, H1 y H2.
. "$PSScriptRoot\comun.ps1"
$init = "greedy"
$csv = Iniciar-Csv "exp1_operadores.csv"
foreach ($ops in "mover", "mover+swap", "mover+vaciar", "mover+swap+vaciar") {
    foreach ($heur in "h1", "h2") {
        foreach ($s in $Semillas) {
            Ejecutar $csv @("exp=1", "seed=$s", "alg=HC", "init=$init", "ops=$ops", "heur=$heur", "alfa=10")
        }
    }
}

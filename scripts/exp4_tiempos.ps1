# Experimento 4: evolución del tiempo de Hill Climbing con el tamaño del problema.
. "$PSScriptRoot\comun.ps1"
$ops = "mover+swap+vaciar"; $init = "greedy"; $heur = "h2"
$csv = Iniciar-Csv "exp4_tiempos.csv"
# 4a: 40 centrales, clientes de 250 en 250. A partir de ~1500 clientes los garantizados ya no caben
# en las 40 centrales (no existe solución válida), así que la serie llega hasta 1250.
foreach ($cli in 250, 500, 750, 1000, 1250) {
    foreach ($s in $Semillas) {
        Ejecutar $csv @("exp=4clientes", "seed=$s", "cli=$cli", "cen=5,10,25", "alg=HC", "init=$init", "ops=$ops", "heur=$heur")
    }
}
# 4b: 1000 clientes, centrales de 40 en 40 manteniendo la proporción 5:10:25
foreach ($m in 1, 2, 3, 4, 5) {
    $cen = "$(5*$m),$(10*$m),$(25*$m)"
    foreach ($s in $Semillas) {
        Ejecutar $csv @("exp=4centrales", "seed=$s", "cli=1000", "cen=$cen", "alg=HC", "init=$init", "ops=$ops", "heur=$heur")
    }
}

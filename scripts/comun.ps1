# Funciones comunes para los scripts de experimentos. No se ejecuta directamente.
$ErrorActionPreference = "Stop"
$Raiz = Split-Path -Parent $PSScriptRoot
Set-Location $Raiz
# Semillas: 1..10, o las de la variable de entorno SEMILLAS (p. ej. $env:SEMILLAS="1 2 3")
if ($env:SEMILLAS) { $Semillas = $env:SEMILLAS -split ' ' | ForEach-Object { [int]$_ } } else { $Semillas = 1..10 }

function Iniciar-Csv($nombre) {
    # con $env:SUFIJO="x" el fichero es expN_..._x.csv (para repartir semillas)
    if ($env:SUFIJO) { $nombre = $nombre -replace '\.csv$', "_$($env:SUFIJO).csv" }
    New-Item -ItemType Directory -Force resultados | Out-Null
    $ruta = Join-Path "resultados" $nombre
    java -cp "bin;lib/*" energia.Main header=true | Out-File -Encoding utf8 $ruta
    return $ruta
}

# Ejecuta una búsqueda y añade su línea al CSV. $args son los parámetros clave=valor del Main.
function Ejecutar($ruta, [string[]]$parametros) {
    Write-Host ("  " + ($parametros -join " "))
    java -Xmx2g -cp "bin;lib/*" energia.Main @parametros | Out-File -Append -Encoding utf8 $ruta
}

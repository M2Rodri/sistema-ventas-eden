# Programa el respaldo automático de la base para que corra solo todos los días en Windows.
#
#   powershell -ExecutionPolicy Bypass -File scripts\programar-respaldo.ps1              (todos los días a las 21:00)
#   powershell -ExecutionPolicy Bypass -File scripts\programar-respaldo.ps1 -Hora 13:30
#   powershell -ExecutionPolicy Bypass -File scripts\programar-respaldo.ps1 -Ahora       (además, lo corre ya mismo)
#   powershell -ExecutionPolicy Bypass -File scripts\programar-respaldo.ps1 -Quitar      (elimina la tarea)
#
# Antes hay que crear el archivo de credenciales (plantilla: scripts\respaldar-base.env.example).
#
# La tarea corre con TU usuario, sin guardar ninguna contraseña de Windows, así que funciona cuando
# tenés la sesión iniciada. Si la computadora estaba apagada a la hora programada, corre apenas se
# prende (-StartWhenAvailable). Si falla (sin internet, por ejemplo), reintenta 3 veces cada 15 minutos.
# El resultado de cada corrida queda en  ..\respaldos\respaldo.log

param(
    [string]$Hora = '21:00',
    [switch]$Quitar,
    [switch]$Ahora
)

$nombre = 'Respaldo Muebleria Eden'

if ($Quitar) {
    if (Get-ScheduledTask -TaskName $nombre -ErrorAction SilentlyContinue) {
        Unregister-ScheduledTask -TaskName $nombre -Confirm:$false
        Write-Host "Tarea '$nombre' eliminada."
    } else {
        Write-Host "No había ninguna tarea '$nombre'."
    }
    return
}

$node = (Get-Command node -ErrorAction SilentlyContinue).Source
if (-not $node) {
    Write-Error 'No encuentro Node.js en el PATH. Instalalo o abrí una terminal donde funcione el comando node.'
    return
}

$script = Join-Path $PSScriptRoot 'respaldar-base.mjs'
$proyecto = Split-Path $PSScriptRoot -Parent
$credenciales = Join-Path (Split-Path $proyecto -Parent) 'credenciales-supabase.env'
if (-not (Test-Path $credenciales)) {
    Write-Warning "Todavía no existe $credenciales. La tarea se crea igual, pero va a fallar hasta que lo crees (plantilla: scripts\respaldar-base.env.example)."
}

$accion = New-ScheduledTaskAction -Execute $node -Argument "`"$script`"" -WorkingDirectory $proyecto
$disparador = New-ScheduledTaskTrigger -Daily -At $Hora
$configuracion = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 15) `
    -ExecutionTimeLimit (New-TimeSpan -Minutes 30) `
    -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries

Register-ScheduledTask -TaskName $nombre -Action $accion -Trigger $disparador -Settings $configuracion `
    -Description 'Copia diaria de la base de datos de Muebleria Eden (scripts/respaldar-base.mjs)' -Force | Out-Null

Write-Host "Tarea '$nombre' programada todos los días a las $Hora."
Write-Host "Para verla: Programador de tareas > Biblioteca del Programador de tareas."
Write-Host "El resultado de cada corrida queda en: $(Join-Path (Split-Path $proyecto -Parent) 'respaldos\respaldo.log')"

if ($Ahora) {
    Write-Host 'Corriendo el respaldo ahora...'
    Start-ScheduledTask -TaskName $nombre
}

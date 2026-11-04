param(
    [string]$MysqlHome = 'C:\laragon\bin\mysql\mysql-8.0.30-winx64',
    [string]$Python = 'python',
    [ValidateRange(1024,65535)][int]$Port = 33307
)
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskId = [Guid]::NewGuid().ToString('N').Substring(0,12)
$taskRun = Join-Path $taskRoot "database\.local\run-$taskId"
$taskData = Join-Path $taskRun 'data'
$taskMysqld = Join-Path $MysqlHome 'bin\mysqld.exe'
$taskMysql = Join-Path $MysqlHome 'bin\mysql.exe'
$taskAdmin = Join-Path $MysqlHome 'bin\mysqladmin.exe'
if (!(Test-Path -LiteralPath $taskMysqld)) { throw "No existe mysqld en $MysqlHome" }
# Refuse to use or modify an already listening instance.
$taskProbe = [Net.Sockets.TcpClient]::new()
try { $taskProbe.Connect('127.0.0.1',$Port); throw "Puerto $Port ocupado; elige otro puerto para la instancia aislada." }
catch [Net.Sockets.SocketException] { }
finally { $taskProbe.Dispose() }
New-Item -ItemType Directory -Path $taskRun -Force | Out-Null
$taskDataArg = $taskData.Replace('\','/')
$taskBaseArg = ([IO.Path]::GetFullPath($MysqlHome)).Replace('\','/')
& $taskMysqld --no-defaults --initialize-insecure "--basedir=$taskBaseArg" "--datadir=$taskDataArg" --console *> (Join-Path $taskRun 'initialize.log')
if ($LASTEXITCODE -ne 0) { throw "Inicializacion fallida; revisar $taskRun\initialize.log" }
$taskProcess = $null
$taskOldPassword = $env:MYSQL_PWD
$taskPasswordSet = $false
$taskExit = 1
try {
    $taskArgs = @('--no-defaults',"--basedir=`"$taskBaseArg`"","--datadir=`"$taskDataArg`"",'--bind-address=127.0.0.1',"--port=$Port",'--mysqlx=OFF','--skip-log-bin','--default-time-zone=+00:00','--innodb-buffer-pool-size=64M','--console')
    $taskProcess = Start-Process -FilePath $taskMysqld -ArgumentList $taskArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $taskRun 'server.stdout.log') -RedirectStandardError (Join-Path $taskRun 'server.stderr.log')
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    $taskReady = $false
    for ($taskAttempt=0; $taskAttempt -lt 120; $taskAttempt++) {
        if ($taskProcess.HasExited) { throw "MySQL termino; revisar $taskRun\server.stderr.log" }
        $taskReportedData = & $taskMysql --no-defaults --protocol=TCP --host=127.0.0.1 "--port=$Port" --user=root --batch --raw --skip-column-names --connect-timeout=1 --execute='SELECT @@datadir' 2>$null
        if ($LASTEXITCODE -eq 0) { $taskReady=$true; break }
        Start-Sleep -Milliseconds 250
    }
    if (!$taskReady) { throw 'MySQL no respondio dentro del plazo de arranque' }
    if (($taskReportedData.Trim().Replace('\','/').TrimEnd('/')) -ine $taskDataArg.TrimEnd('/')) { throw 'La instancia no utiliza el datadir aislado esperado' }
    # Random credential only for this disposable local instance. Never printed or placed in argv.
    $taskPassword = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    "ALTER USER 'root'@'localhost' IDENTIFIED BY '$taskPassword';" | & $taskMysql --no-defaults --protocol=TCP --host=127.0.0.1 "--port=$Port" --user=root
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo proteger la cuenta de pruebas' }
    $env:MYSQL_PWD = $taskPassword
    $taskPasswordSet = $true
    & $Python (Join-Path $taskRoot 'database\tests\test_schema.py') --mysql-bin $taskMysql --port $Port --database "sc_test_$taskId" --report (Join-Path $taskRoot 'database\tests\evidence\resultado_mysql.json')
    $taskExit = $LASTEXITCODE
} finally {
    if ($taskProcess -and !$taskProcess.HasExited) {
        & $taskAdmin --no-defaults --protocol=TCP --host=127.0.0.1 "--port=$Port" --user=root --connect-timeout=3 shutdown 2>$null
        if ($LASTEXITCODE -ne 0) {
            # Stop only the exact process created above; never a Laragon/Windows service.
            Stop-Process -Id $taskProcess.Id -ErrorAction SilentlyContinue
        } else { $taskProcess.WaitForExit(15000) | Out-Null }
    }
    if ($null -eq $taskOldPassword) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    else { $env:MYSQL_PWD = $taskOldPassword }
}
Write-Output "Instancia aislada detenida. Logs locales: $taskRun"
exit $taskExit

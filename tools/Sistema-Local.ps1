param(
    [ValidateSet('Iniciar','Detener','Reiniciar','Estado')][string]$Accion='Iniciar',
    [string]$MySqlHome='C:\laragon\bin\mysql\mysql-8.0.30-winx64',
    [string]$JavaHome='C:\Program Files\Android\Android Studio\jbr'
)
$ErrorActionPreference='Stop'
$taskRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskLocal=Join-Path $taskRoot 'backend\.local\desarrollo'
$taskData=Join-Path $taskLocal 'mysql\data'
$taskConfigPath=Join-Path $taskLocal 'configuracion.local.json'
$taskEnvPath=Join-Path $taskRoot 'backend\.env'
$taskAccessPath=Join-Path $taskLocal 'acceso-administrador.txt'
$taskDbClient=Join-Path $taskLocal 'mysql\cliente-admin.cnf'
$taskApiPid=Join-Path $taskLocal 'api.proceso.json'
$taskRuntimeJar=Join-Path $taskLocal 'api\santa-cecilia-api.jar'
$taskJava=Join-Path $JavaHome 'bin\java.exe'
$taskMysql=Join-Path $MySqlHome 'bin\mysql.exe'
$taskMysqld=Join-Path $MySqlHome 'bin\mysqld.exe'
$taskConfig=$null
if(Test-Path -LiteralPath $taskConfigPath){$taskConfig=Get-Content -Raw -LiteralPath $taskConfigPath | ConvertFrom-Json}

function Save-PrivateText([string]$Path,[string]$Value){
    [IO.File]::WriteAllText($Path,$Value,[Text.UTF8Encoding]::new($false))
    $taskSid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    & icacls.exe $Path '/inheritance:r' '/grant:r' "*$($taskSid):(F)" '*S-1-5-18:(F)' | Out-Null
    if($LASTEXITCODE -ne 0){throw "No se pudo proteger el archivo local: $Path"}
}
function New-Secret {
    $taskBytes=New-Object byte[] 24
    $taskRng=[Security.Cryptography.RandomNumberGenerator]::Create()
    try{$taskRng.GetBytes($taskBytes)}finally{$taskRng.Dispose()}
    return ([BitConverter]::ToString($taskBytes)).Replace('-','').ToLowerInvariant()
}
function Port-Open([int]$Port){
    $taskSocket=New-Object Net.Sockets.TcpClient
    try{$taskConnect=$taskSocket.BeginConnect('127.0.0.1',$Port,$null,$null);if(!$taskConnect.AsyncWaitHandle.WaitOne(500)){return $false};$taskSocket.EndConnect($taskConnect);return $true}catch{return $false}finally{$taskSocket.Dispose()}
}
function Query-OwnDatabase([string]$Sql,[switch]$EmptyPassword){
    $taskArgs=@("--defaults-extra-file=$taskDbClient",'--protocol=TCP','--host=127.0.0.1',"--port=$($taskConfig.dbPort)",'--user=root','--batch','--raw','--skip-column-names','--connect-timeout=2')
    if($EmptyPassword){$taskArgs=@('--no-defaults','--protocol=TCP','--host=127.0.0.1',"--port=$($taskConfig.dbPort)",'--user=root','--skip-password','--batch','--raw','--skip-column-names','--connect-timeout=2')}
    # Windows PowerShell 5 convierte avisos de stderr en errores; el resultado real es ExitCode.
    $taskPreviousPreference=$ErrorActionPreference
    try{
        $ErrorActionPreference='Continue'
        $taskResult=$Sql | & $taskMysql @taskArgs 2> (Join-Path $taskLocal 'mysql\cliente-error.log')
        $taskExit=$LASTEXITCODE
    }finally{$ErrorActionPreference=$taskPreviousPreference}
    if($taskExit -ne 0){throw 'No se pudo consultar el MySQL local del proyecto. Revisa backend/.local/desarrollo/mysql/cliente-error.log'}
    return ($taskResult -join "`n").Trim()
}
function Assert-OwnDatabase([switch]$EmptyPassword){
    $taskActual=Query-OwnDatabase 'SELECT @@datadir;' -EmptyPassword:$EmptyPassword
    if($taskActual.Replace('\','/').TrimEnd('/') -ine $taskData.Replace('\','/').TrimEnd('/')){throw 'El puerto pertenece a otra base de datos. No se modificara ni detendra ese servidor.'}
}
function Initialize-Configuration {
    if($null -ne $script:taskConfig){return}
    if(Test-Path -LiteralPath $taskEnvPath){throw 'backend/.env ya existe y no pertenece al entorno administrado. Se conserva. Usa tools/Iniciar-Backend.ps1 con tu MySQL configurado o revisa docs/ARRANQUE_LOCAL.md.'}
    if(Test-Path -LiteralPath $taskData){throw 'Ya existe el directorio de datos sin configuracion administrada. Se conserva; no se inicializa ni se borra.'}
    if(Port-Open 33306){throw 'El puerto 33306 esta ocupado. No se iniciara ni modificara otra base.'}
    New-Item -ItemType Directory -Path $taskLocal,(Join-Path $taskLocal 'mysql'),(Join-Path $taskLocal 'api') -Force | Out-Null
    $script:taskConfig=[PSCustomObject]@{workspace=$taskRoot;dbPort=33306;schema='santa_cecilia';rootPassword=(New-Secret);appPassword=(New-Secret);migrationPassword=(New-Secret);adminEmail='administrador@santacecilia.local';adminPassword=('SCa!'+(New-Secret).Substring(0,24));dbPrepared=$false;mysqlHome=$MySqlHome}
    Save-PrivateText $taskConfigPath ($script:taskConfig | ConvertTo-Json)
    Save-PrivateText $taskDbClient ("[client]`nuser=root`npassword="+$script:taskConfig.rootPassword+"`nhost=127.0.0.1`nport=33306`nprotocol=TCP`n")
    $taskEnv=@(
        '# Configuracion LOCAL persistente generada por tools/Sistema-Local.ps1. No compartir.',
        'DB_URL=jdbc:mysql://127.0.0.1:33306/santa_cecilia?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&sslMode=PREFERRED',
        'DB_USERNAME=sc_app',('DB_PASSWORD='+$script:taskConfig.appPassword),
        'DB_MIGRATION_USERNAME=sc_migrate',('DB_MIGRATION_PASSWORD='+$script:taskConfig.migrationPassword),
        'SERVER_ADDRESS=127.0.0.1','SERVER_PORT=8443','SPRING_PROFILES_ACTIVE=https',
        'APP_BOOTSTRAP_ENABLED=true',('APP_BOOTSTRAP_EMAIL='+$script:taskConfig.adminEmail),('APP_BOOTSTRAP_PASSWORD='+$script:taskConfig.adminPassword),
        'APP_BOOTSTRAP_NOMBRES=Administrador','APP_BOOTSTRAP_APELLIDOS=Santa Cecilia','APP_PUBLIC_URL=https://localhost:8443',
        'SMTP_HOST=','SMTP_PORT=587','SMTP_USERNAME=','SMTP_PASSWORD=','SMTP_FROM=','SMTP_AUTH=true','SMTP_STARTTLS=true'
    ) -join "`r`n"
    Save-PrivateText $taskEnvPath ($taskEnv+"`r`n")
    Save-PrivateText $taskAccessPath ("SANTA CECILIA - ACCESO LOCAL`r`n`r`nCorreo: "+$script:taskConfig.adminEmail+"`r`nContrasena inicial: "+$script:taskConfig.adminPassword+"`r`n`r`nInicia INICIAR_SANTA_CECILIA.cmd antes de abrir la app.`r`nNo se restablece la contrasena al reiniciar. No compartas este archivo.`r`n")
}
function Start-Database {
    if($taskConfig.workspace -ine $taskRoot){throw 'La configuracion corresponde a otra carpeta. No se reutilizaran sus datos automaticamente.'}
    if(!(Test-Path -LiteralPath (Join-Path $taskData 'auto.cnf'))){
        if((Test-Path -LiteralPath $taskData) -and (Get-ChildItem -LiteralPath $taskData -Force | Select-Object -First 1)){throw 'El directorio de datos no esta vacio; se conserva sin reinicializar.'}
        Write-Output 'Creando MySQL persistente del proyecto (una sola vez)...'
        $taskInit=Start-Process -FilePath $taskMysqld -ArgumentList @('--no-defaults','--initialize-insecure',('"--basedir='+$MySqlHome.Replace('\','/')+'"'),('"--datadir='+$taskData.Replace('\','/')+'"'),'--console') -WindowStyle Hidden -Wait -PassThru -RedirectStandardOutput (Join-Path $taskLocal 'mysql\inicializacion.log') -RedirectStandardError (Join-Path $taskLocal 'mysql\inicializacion-error.log')
        if($taskInit.ExitCode -ne 0){throw 'Fallo la inicializacion local; revisa backend/.local/desarrollo/mysql/inicializacion-error.log. No se borraron datos.'}
    }
    if(!(Port-Open $taskConfig.dbPort)){
        $taskDbProcess=Start-Process -FilePath $taskMysqld -ArgumentList @('--no-defaults',('"--basedir='+$MySqlHome.Replace('\','/')+'"'),('"--datadir='+$taskData.Replace('\','/')+'"'),'--bind-address=127.0.0.1',"--port=$($taskConfig.dbPort)",'--mysqlx=OFF','--skip-log-bin','--default-time-zone=+00:00','--console') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $taskLocal 'mysql\servidor.log') -RedirectStandardError (Join-Path $taskLocal 'mysql\servidor-error.log')
        for($taskAttempt=0;$taskAttempt -lt 100;$taskAttempt++){if(Port-Open $taskConfig.dbPort){break};if($taskDbProcess.HasExited){throw 'MySQL termino al iniciar; revisa el registro local'};Start-Sleep -Milliseconds 250}
    }
    if(!$taskConfig.dbPrepared){
        $taskProtected=$true
        try{Assert-OwnDatabase}catch{$taskProtected=$false;Assert-OwnDatabase -EmptyPassword}
        if(!$taskProtected){Query-OwnDatabase ("ALTER USER 'root'@'localhost' IDENTIFIED BY '"+$taskConfig.rootPassword+"';") -EmptyPassword | Out-Null}
        Assert-OwnDatabase
        $taskSql="CREATE DATABASE IF NOT EXISTS santa_cecilia CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER IF NOT EXISTS 'sc_app'@'127.0.0.1' IDENTIFIED BY '"+$taskConfig.appPassword+"'; CREATE USER IF NOT EXISTS 'sc_migrate'@'127.0.0.1' IDENTIFIED BY '"+$taskConfig.migrationPassword+"'; GRANT SELECT,INSERT,UPDATE ON santa_cecilia.* TO 'sc_app'@'127.0.0.1'; GRANT ALL PRIVILEGES ON santa_cecilia.* TO 'sc_migrate'@'127.0.0.1';"
        Query-OwnDatabase $taskSql | Out-Null
        $taskConfig.dbPrepared=$true
        Save-PrivateText $taskConfigPath ($taskConfig | ConvertTo-Json)
    }else{Assert-OwnDatabase}
}
function Read-ApiProcess {
    if(!(Test-Path -LiteralPath $taskApiPid)){return $null}
    $taskInfo=Get-Content -Raw -LiteralPath $taskApiPid | ConvertFrom-Json
    $taskProcess=Get-Process -Id $taskInfo.pid -ErrorAction SilentlyContinue
    if(!$taskProcess){return $null}
    if($taskProcess.Path -ine $taskJava -or [string]$taskProcess.StartTime.ToUniversalTime().Ticks -ne [string]$taskInfo.startedTicks){throw 'El PID guardado fue reutilizado; no se controlara ese proceso.'}
    $taskCommand=(Get-CimInstance Win32_Process -Filter "ProcessId=$($taskInfo.pid)").CommandLine
    if(!$taskCommand -or !$taskCommand.Contains($taskRuntimeJar)){throw 'El proceso no corresponde al JAR local del proyecto.'}
    return $taskProcess
}
function Initialize-Probe {
    if('SantaCeciliaLocalProbe' -as [type]){return}
    Add-Type -TypeDefinition @'
using System;
using System.IO;
using System.Net;
using System.Net.Security;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text.RegularExpressions;
public static class SantaCeciliaLocalProbe {
    public static string Get(string url,string pem) {
        string body=Regex.Replace(File.ReadAllText(pem),"-----BEGIN CERTIFICATE-----|-----END CERTIFICATE-----|\\s", "");
        var expected=new X509Certificate2(Convert.FromBase64String(body));
        ServicePointManager.SecurityProtocol=SecurityProtocolType.Tls12;
        var request=(HttpWebRequest)WebRequest.Create(url); request.Proxy=null;request.Timeout=2000;
        request.ServerCertificateValidationCallback=(sender,cert,chain,errors)=> {
            if(cert==null || (errors & (SslPolicyErrors.RemoteCertificateNameMismatch|SslPolicyErrors.RemoteCertificateNotAvailable))!=0)return false;
            var actual=new X509Certificate2(cert);
            return actual.Thumbprint==expected.Thumbprint && DateTime.UtcNow>=actual.NotBefore.ToUniversalTime() && DateTime.UtcNow<=actual.NotAfter.ToUniversalTime();
        };
        using(var response=request.GetResponse())using(var reader=new StreamReader(response.GetResponseStream()))return reader.ReadToEnd();
    }
}
'@
}
function Api-Ready {
    try{
        Initialize-Probe
        $taskBody=[SantaCeciliaLocalProbe]::Get('https://127.0.0.1:8443/api/v1/health',(Join-Path $taskRoot 'app\src\debug\res\raw\desarrollo_ca.pem')) | ConvertFrom-Json
        return $taskBody.data.estado -eq 'DISPONIBLE'
    }catch{return $false}
}
function Start-Api {
    if(Read-ApiProcess){if(Api-Ready){Write-Output 'El backend ya esta iniciado.';return};throw 'El proceso del backend existe pero no esta disponible. Revisa su registro o usa REINICIAR_SANTA_CECILIA.cmd.'}
    if(Port-Open 8443){throw 'El puerto 8443 pertenece a otro proceso. No se detendra ese proceso.'}
    $taskJar=Join-Path $taskRoot 'backend\build\libs\santa-cecilia-api.jar'
    if(!(Test-Path -LiteralPath $taskJar)){
        $env:JAVA_HOME=$JavaHome
        Push-Location $taskRoot
        try{& (Join-Path $taskRoot 'gradlew.bat') -p backend bootJar --console=plain;if($LASTEXITCODE -ne 0){throw 'No se pudo compilar el backend'}}finally{Pop-Location}
    }
    & (Join-Path $PSScriptRoot 'Preparar-HttpsLocal.ps1') -JavaHome $JavaHome
    Copy-Item -LiteralPath $taskJar -Destination $taskRuntimeJar -Force
    foreach($taskLine in [IO.File]::ReadAllLines($taskEnvPath)){
        if(!$taskLine.Trim() -or $taskLine.Trim().StartsWith('#')){continue}
        $taskParts=$taskLine.Split('=',2)
        if($taskParts.Count -ne 2 -or $taskParts[0] -notmatch '^[A-Z][A-Z0-9_]*$'){throw 'Variable local no valida'}
        [Environment]::SetEnvironmentVariable($taskParts[0],$taskParts[1],'Process')
    }
    $env:TLS_KEY_STORE=[Uri]::new((Join-Path $taskRoot 'backend\.local\tls\desarrollo.p12')).AbsoluteUri
    $env:TLS_KEY_STORE_PASSWORD=[IO.File]::ReadAllText((Join-Path $taskRoot 'backend\.local\tls\clave-local.txt'))
    $taskProcess=Start-Process -FilePath $taskJava -ArgumentList @('-jar',('"'+$taskRuntimeJar+'"')) -WorkingDirectory (Join-Path $taskRoot 'backend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $taskLocal 'api\servidor.log') -RedirectStandardError (Join-Path $taskLocal 'api\servidor-error.log')
    [IO.File]::WriteAllText($taskApiPid,(@{pid=$taskProcess.Id;startedTicks=[string]$taskProcess.StartTime.ToUniversalTime().Ticks} | ConvertTo-Json))
    Write-Output 'Iniciando API HTTPS y aplicando las migraciones de la base...'
    for($taskAttempt=0;$taskAttempt -lt 90;$taskAttempt++){
        if($taskProcess.HasExited){throw 'El backend termino. Revisa backend/.local/desarrollo/api/servidor.log'}
        if(Api-Ready){
            $taskCount=Query-OwnDatabase "SELECT COUNT(*) FROM santa_cecilia.usuario WHERE id_rol=1 AND estado='ACTIVO';"
            if([int]$taskCount -gt 0){break}
        }
        Start-Sleep -Milliseconds 500
    }
    if(!(Api-Ready)){throw 'La API todavia no esta disponible. Revisa backend/.local/desarrollo/api/servidor.log'}
    # Las tablas se crean por Flyway; los permisos de borrado quedan limitados a contenido editable.
    foreach($taskTable in @('servicio_genero','servicio_instrumento','servicio_tipo_evento','detalle_cotizacion','imagen_servicio','horario_atencion')){Query-OwnDatabase "GRANT DELETE ON santa_cecilia.$taskTable TO 'sc_app'@'127.0.0.1';" | Out-Null}
}
function Stop-Local {
    $taskProcess=Read-ApiProcess
    if($taskProcess){Stop-Process -Id $taskProcess.Id;$taskProcess.WaitForExit(15000) | Out-Null;Write-Output 'Backend local detenido.'}
    if($taskConfig -and (Port-Open $taskConfig.dbPort)){
        Assert-OwnDatabase
        $taskDbProcess=$null
        $taskPidFile=Query-OwnDatabase 'SELECT @@pid_file;'
        $taskPidResolved=[IO.Path]::GetFullPath($taskPidFile)
        if(!$taskPidResolved.StartsWith($taskData.TrimEnd('\')+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'El PID de MySQL no pertenece al directorio administrado.'}
        if(Test-Path -LiteralPath $taskPidResolved){
            $taskDbProcess=Get-Process -Id ([int]([IO.File]::ReadAllText($taskPidResolved).Trim())) -ErrorAction SilentlyContinue
            if($taskDbProcess -and $taskDbProcess.Path -ine $taskMysqld){throw 'El PID de MySQL no corresponde al ejecutable esperado.'}
        }
        $taskPreviousPreference=$ErrorActionPreference
        try{
            $ErrorActionPreference='Continue'
            & (Join-Path $MySqlHome 'bin\mysqladmin.exe') "--defaults-extra-file=$taskDbClient" '--protocol=TCP' '--host=127.0.0.1' "--port=$($taskConfig.dbPort)" '--user=root' 'shutdown' 2> (Join-Path $taskLocal 'mysql\detener-error.log')
            $taskExit=$LASTEXITCODE
        }finally{$ErrorActionPreference=$taskPreviousPreference}
        if($taskExit -ne 0){throw 'No se pudo detener MySQL de forma normal; no se forzara ni borrara nada.'}
        if($taskDbProcess -and !$taskDbProcess.WaitForExit(20000)){throw 'MySQL esta terminando de guardar sus datos. Espera antes de volver a iniciarlo.'}
        Write-Output 'MySQL local detenido. Los datos se conservan.'
    }
}

if(!(Test-Path -LiteralPath $taskJava)){throw 'No se encontro el JDK de Android Studio. Indica -JavaHome.'}
if(!(Test-Path -LiteralPath $taskMysqld)){throw 'No se encontro MySQL de Laragon. Indica -MySqlHome con su carpeta de binarios.'}
if($Accion -eq 'Estado'){
    if(!$taskConfig){Write-Output 'Entorno sin preparar. Ejecuta INICIAR_SANTA_CECILIA.cmd.';exit 0}
    Write-Output ("MySQL del proyecto (33306): "+(Port-Open $taskConfig.dbPort))
    Write-Output ("API HTTPS (8443): "+(Api-Ready))
    Write-Output ("Acceso inicial: "+$taskAccessPath)
    exit 0
}
if($Accion -eq 'Detener'){Stop-Local;exit 0}
if($Accion -eq 'Reiniciar'){Stop-Local}
Initialize-Configuration
Start-Database
Start-Api
Write-Output ''
Write-Output 'SANTA CECILIA LISTO. Puedes ejecutar app desde Android Studio.'
Write-Output 'MySQL persistente: 127.0.0.1:33306 / santa_cecilia'
Write-Output 'API del emulador: https://10.0.2.2:8443/api/v1/'
Write-Output ("Correo y contrasena inicial del administrador: "+$taskAccessPath)
Write-Output 'Los servidores siguen funcionando al cerrar esta consola. Para detenerlos usa DETENER_SANTA_CECILIA.cmd.'

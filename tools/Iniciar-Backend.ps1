param(
    [string]$EnvironmentFile,
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$HttpsLocal
)
$ErrorActionPreference='Stop'
$taskRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if($EnvironmentFile){
    $taskEnvPath=[IO.Path]::GetFullPath($EnvironmentFile)
    if(!(Test-Path -LiteralPath $taskEnvPath)){throw "No existe el archivo de entorno: $taskEnvPath"}
    foreach($taskLine in [IO.File]::ReadAllLines($taskEnvPath)){
        $taskValueLine=$taskLine.Trim()
        if(!$taskValueLine -or $taskValueLine.StartsWith('#')){continue}
        $taskParts=$taskValueLine.Split('=',2)
        if($taskParts.Count -ne 2 -or $taskParts[0] -notmatch '^[A-Z][A-Z0-9_]*$'){throw 'Formato de variable de entorno no valido'}
        [Environment]::SetEnvironmentVariable($taskParts[0],$taskParts[1],'Process')
    }
}
foreach($taskName in @('DB_URL','DB_USERNAME','DB_PASSWORD','DB_MIGRATION_USERNAME','DB_MIGRATION_PASSWORD')){
    $taskValue=[Environment]::GetEnvironmentVariable($taskName,'Process')
    if([string]::IsNullOrWhiteSpace($taskValue) -or $taskValue.StartsWith('REEMPLAZAR')){throw "Configura $taskName antes de iniciar el backend"}
}
if(!$JavaHome){$JavaHome='C:\Program Files\Android\Android Studio\jbr'}
$taskJava=Join-Path $JavaHome 'bin\java.exe'
if(!(Test-Path -LiteralPath $taskJava)){throw 'Selecciona un JDK 21 o superior mediante -JavaHome'}
$taskJar=Join-Path $taskRoot 'backend\build\libs\santa-cecilia-api.jar'
if(!(Test-Path -LiteralPath $taskJar)){throw 'Primero compila: .\gradlew.bat -p backend bootJar'}
if($HttpsLocal){
    & (Join-Path $PSScriptRoot 'Preparar-HttpsLocal.ps1') -JavaHome $JavaHome
    $taskProfiles=@($env:SPRING_PROFILES_ACTIVE -split ',' | ForEach-Object {$_.Trim()} | Where-Object {$_ -and $_ -ne 'https'})
    $env:SPRING_PROFILES_ACTIVE=(@($taskProfiles)+@('https')) -join ','
    $env:SERVER_PORT='8443'
    $env:SERVER_ADDRESS='127.0.0.1'
    $env:TLS_KEY_STORE=[Uri]::new((Join-Path $taskRoot 'backend\.local\tls\desarrollo.p12')).AbsoluteUri
    $env:TLS_KEY_STORE_PASSWORD=[IO.File]::ReadAllText((Join-Path $taskRoot 'backend\.local\tls\clave-local.txt'))
    Write-Output 'Backend HTTPS local: https://localhost:8443/api/v1/ (emulador: https://10.0.2.2:8443/api/v1/)'
}
& $taskJava -jar $taskJar
exit $LASTEXITCODE

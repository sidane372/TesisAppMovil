param([string]$JavaHome='C:\Program Files\Android\Android Studio\jbr')
$ErrorActionPreference='Stop'
$taskRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskTls=Join-Path $taskRoot 'backend\.local\tls'
$taskRaw=Join-Path $taskRoot 'app\src\debug\res\raw'
$taskKeytool=Join-Path $JavaHome 'bin\keytool.exe'
if(!(Test-Path -LiteralPath $taskKeytool)){throw 'Selecciona el JDK mediante -JavaHome'}
New-Item -ItemType Directory -Path $taskTls,$taskRaw -Force | Out-Null
$taskStore=Join-Path $taskTls 'desarrollo.p12';$taskPasswordFile=Join-Path $taskTls 'clave-local.txt'
if(!(Test-Path -LiteralPath $taskStore)){
    if(Test-Path -LiteralPath $taskPasswordFile){throw 'Existe clave local sin certificado. Revisa backend/.local/tls antes de regenerar.'}
    $taskSecret=[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')
    [IO.File]::WriteAllText($taskPasswordFile,$taskSecret)
}else{
    if(!(Test-Path -LiteralPath $taskPasswordFile)){throw 'Falta la clave del certificado de desarrollo local'}
    $taskSecret=[IO.File]::ReadAllText($taskPasswordFile)
}
$taskPrevious=$env:SC_LOCAL_TLS_PASSWORD
function Invoke-LocalKeytool([string[]]$Arguments){
    $taskPreviousPreference=$ErrorActionPreference
    try{
        $ErrorActionPreference='Continue'
        & $taskKeytool @Arguments 2> (Join-Path $taskTls 'keytool.log')
        $taskExit=$LASTEXITCODE
    }finally{$ErrorActionPreference=$taskPreviousPreference}
    if($taskExit -ne 0){throw 'No se pudo preparar el certificado. Revisa backend/.local/tls/keytool.log'}
}
try{
    $env:SC_LOCAL_TLS_PASSWORD=$taskSecret
    if(!(Test-Path -LiteralPath $taskStore)){
        Invoke-LocalKeytool @('-genkeypair','-alias','localdev','-keyalg','RSA','-keysize','2048','-validity','825','-storetype','PKCS12','-keystore',$taskStore,'-storepass:env','SC_LOCAL_TLS_PASSWORD','-dname','CN=Santa Cecilia Desarrollo Local','-ext','SAN=ip:10.0.2.2,ip:127.0.0.1,dns:localhost','-ext','bc=ca:true','-ext','KU=digitalSignature,keyEncipherment,keyCertSign','-ext','EKU=serverAuth','-noprompt')
    }
    Invoke-LocalKeytool @('-exportcert','-rfc','-alias','localdev','-keystore',$taskStore,'-storepass:env','SC_LOCAL_TLS_PASSWORD','-file',(Join-Path $taskRaw 'desarrollo_ca.pem'))
}finally{
    if($null -eq $taskPrevious){Remove-Item Env:SC_LOCAL_TLS_PASSWORD -ErrorAction SilentlyContinue}else{$env:SC_LOCAL_TLS_PASSWORD=$taskPrevious}
}
Write-Output 'HTTPS local preparado. Clave privada: backend/.local/tls (ignorada por Git). Solo la compilacion debug confia en este certificado para localhost/10.0.2.2.'

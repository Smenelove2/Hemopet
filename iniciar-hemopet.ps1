[CmdletBinding()]
param([switch]$Preparar)

$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$runtimeRoot = Join-Path $projectRoot '.hemopet-runtime'
$dataDir = Join-Path $runtimeRoot 'mysql-data'
$libDir = Join-Path $runtimeRoot 'lib'
$classesDir = Join-Path $runtimeRoot 'classes'
$logFile = Join-Path $runtimeRoot 'mysql-error.log'
$pidFile = Join-Path $runtimeRoot 'mysql.pid'
$mysqlPort = 3308

function Write-Step([string]$message) {
    Write-Host "[HemoPet] $message" -ForegroundColor Cyan
}

function Find-MySqlBase {
    $candidates = @(
        'C:\Program Files\MySQL\MySQL Server 8.0',
        'C:\Program Files\MySQL\MySQL Server 9.2'
    )

    foreach ($candidate in $candidates) {
        if (Test-Path (Join-Path $candidate 'bin\mysqld.exe')) {
            return $candidate
        }
    }

    throw 'MySQL Server 8 ou 9 nao foi encontrado em C:\Program Files\MySQL.'
}

function Test-MySqlReady([string]$mysqlAdminExe) {
    # No Windows PowerShell 5.1, stderr de um executavel pode interromper o
    # script com ErrorActionPreference=Stop, mesmo com 2>$null. Uma falha
    # nesta sondagem e esperada enquanto o servidor ainda esta iniciando.
    $probeInfo = New-Object System.Diagnostics.ProcessStartInfo
    $probeInfo.FileName = $mysqlAdminExe
    $probeInfo.Arguments = "--no-defaults --protocol=TCP --host=127.0.0.1 --port=$mysqlPort --user=root --connect-timeout=1 ping"
    $probeInfo.UseShellExecute = $false
    $probeInfo.CreateNoWindow = $true
    $probeInfo.RedirectStandardOutput = $true
    $probeInfo.RedirectStandardError = $true

    $probe = [System.Diagnostics.Process]::Start($probeInfo)
    try {
        $outputRead = $probe.StandardOutput.ReadToEndAsync()
        $errorRead = $probe.StandardError.ReadToEndAsync()
        if (-not $probe.WaitForExit(3000)) {
            $probe.Kill()
            $probe.WaitForExit()
            return $false
        }
        return $probe.ExitCode -eq 0
    } finally {
        $probe.Dispose()
    }
}

function Ensure-Download([string]$uri, [string]$destination) {
    if (-not (Test-Path -LiteralPath $destination)) {
        Write-Step "Baixando $(Split-Path $destination -Leaf)..."
        Invoke-WebRequest -Uri $uri -OutFile $destination -UseBasicParsing
    }
}

function Find-JavaTool([string]$name) {
    $javaCandidates = @()
    if ($env:JAVA_HOME) { $javaCandidates += (Join-Path $env:JAVA_HOME "bin\$name.exe") }
    $javaCandidates += @(Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName "bin\$name.exe" })
    foreach ($javaCandidate in $javaCandidates) {
        if (Test-Path -LiteralPath $javaCandidate) { return $javaCandidate }
    }
    return (Get-Command "$name.exe" -ErrorAction Stop).Source
}

New-Item -ItemType Directory -Path $runtimeRoot, $libDir -Force | Out-Null

$mysqlBase = Find-MySqlBase
$mysqldExe = Join-Path $mysqlBase 'bin\mysqld.exe'
$mysqlExe = Join-Path $mysqlBase 'bin\mysql.exe'
$mysqlAdminExe = Join-Path $mysqlBase 'bin\mysqladmin.exe'

if (-not (Test-Path (Join-Path $dataDir 'auto.cnf'))) {
    Write-Step 'Preparando o MySQL local sem senha (primeira execucao)...'
    New-Item -ItemType Directory -Path $dataDir -Force | Out-Null
    & $mysqldExe --no-defaults --initialize-insecure "--basedir=$mysqlBase" "--datadir=$dataDir" --console
    if ($LASTEXITCODE -ne 0) {
        throw 'Nao foi possivel inicializar o MySQL local.'
    }
}

if (-not (Test-MySqlReady $mysqlAdminExe)) {
    Write-Step "Iniciando o MySQL local na porta $mysqlPort..."
    $serverArgs = @(
        '--no-defaults',
        "`"--basedir=$mysqlBase`"",
        "--datadir=$dataDir",
        "--port=$mysqlPort",
        '--bind-address=127.0.0.1',
        '--mysqlx=0',
        '--skip-log-bin',
        "--pid-file=$pidFile",
        "--log-error=$logFile"
    )
    Start-Process -FilePath $mysqldExe -ArgumentList $serverArgs -WindowStyle Hidden | Out-Null

    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        Start-Sleep -Milliseconds 500
        if (Test-MySqlReady $mysqlAdminExe) {
            $ready = $true
            break
        }
    }

    if (-not $ready) {
        if (Test-Path $logFile) {
            Get-Content $logFile -Tail 40
        }
        throw 'O MySQL local nao iniciou corretamente.'
    }
}

$clientArgs = @(
    '--no-defaults',
    '--protocol=TCP',
    '--host=127.0.0.1',
    "--port=$mysqlPort",
    '--user=root',
    '--default-character-set=utf8mb4'
)

$databaseExists = & $mysqlExe @clientArgs --batch --skip-column-names --execute="SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='hemopet';"
if (-not $databaseExists) {
    Write-Step 'Criando o banco HemoPet e carregando os dados iniciais...'
    $schemaScript = (Join-Path $projectRoot 'database\01_criacao_tabelas_hemopet.sql').Replace('\', '/')
    $dataScript = (Join-Path $projectRoot 'database\02_insercao_dados.sql').Replace('\', '/')
    & $mysqlExe @clientArgs --execute="source $schemaScript"
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar as tabelas.' }
    & $mysqlExe @clientArgs --execute="source $dataScript"
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao carregar os dados iniciais.' }
}

Write-Step 'Verificando as atualizacoes das Etapas 04 e 05...'
& $mysqlExe @clientArgs --execute="CREATE TABLE IF NOT EXISTS hemopet.app_schema_migration (script_name VARCHAR(160) PRIMARY KEY, checksum CHAR(64) NOT NULL, applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);"
if ($LASTEXITCODE -ne 0) { throw 'Falha ao preparar o controle de atualizacoes.' }
$updates = @('database/04_consultas_visoes_indices.sql', 'database/etapa5/01_funcoes.sql',
    'database/etapa5/02_procedimentos.sql', 'database/etapa5/03_triggers.sql')
foreach ($update in $updates) {
    $updatePath = Join-Path $projectRoot $update
    $checksum = (Get-FileHash -LiteralPath $updatePath -Algorithm SHA256).Hash
    $installed = & $mysqlExe @clientArgs --batch --skip-column-names --execute="SELECT checksum FROM hemopet.app_schema_migration WHERE script_name='$update';"
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao consultar o controle de atualizacoes.' }
    if ($installed -eq $checksum) { continue }
    Write-Step "Aplicando $update..."
    $sqlPath = $updatePath.Replace('\', '/')
    & $mysqlExe @clientArgs --execute="source $sqlPath" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Falha ao aplicar $update. Os dados existentes foram preservados." }
    & $mysqlExe @clientArgs --execute="INSERT INTO hemopet.app_schema_migration(script_name, checksum) VALUES ('$update', '$checksum') ON DUPLICATE KEY UPDATE checksum='$checksum';"
    if ($LASTEXITCODE -ne 0) { throw "Falha ao registrar $update." }
}

$mysqlJar = Join-Path $libDir 'mysql-connector-j-9.0.0.jar'
$chartJar = Join-Path $libDir 'jfreechart-1.5.5.jar'
Ensure-Download 'https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.0.0/mysql-connector-j-9.0.0.jar' $mysqlJar
Ensure-Download 'https://repo.maven.apache.org/maven2/org/jfree/jfreechart/1.5.5/jfreechart-1.5.5.jar' $chartJar

$javac = Find-JavaTool 'javac'
$javaw = Find-JavaTool 'javaw'

Write-Step 'Compilando o projeto...'
if (Test-Path -LiteralPath $classesDir) {
    $resolvedClasses = [IO.Path]::GetFullPath($classesDir)
    $expectedClasses = [IO.Path]::GetFullPath((Join-Path $projectRoot '.hemopet-runtime\classes'))
    if ($resolvedClasses -ne $expectedClasses -or
        -not $resolvedClasses.StartsWith([IO.Path]::GetFullPath($projectRoot) + [IO.Path]::DirectorySeparatorChar)) {
        throw 'Diretorio de compilacao fora do projeto.'
    }
    Remove-Item -LiteralPath $classesDir -Recurse -Force
}
New-Item -ItemType Directory -Path $classesDir -Force | Out-Null
$sources = (Get-ChildItem (Join-Path $projectRoot 'src\main\java') -Filter '*.java' -Recurse -File).FullName
& $javac --release 17 -encoding UTF-8 -cp "$mysqlJar;$chartJar" -d $classesDir $sources
if ($LASTEXITCODE -ne 0) {
    throw 'A compilacao do projeto falhou.'
}

$env:HEMOPET_DB_URL = "jdbc:mysql://127.0.0.1:$mysqlPort/hemopet?useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true"
$env:HEMOPET_DB_USER = 'root'
Remove-Item Env:HEMOPET_DB_PASSWORD -ErrorAction SilentlyContinue

$resourcesDir = Join-Path $projectRoot 'src\main\resources'
$classpath = "$classesDir;$resourcesDir;$libDir\*"
if ($Preparar) { Write-Step 'Banco e compilacao preparados.'; return }
Write-Step 'Abrindo a aplicacao...'
$application = Start-Process -FilePath $javaw -ArgumentList @('-cp', "`"$classpath`"", 'br.com.hemopet.app.HemoPetApplication') -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru
Start-Sleep -Seconds 2
if ($application.HasExited) {
    throw "A aplicacao encerrou durante a inicializacao (codigo $($application.ExitCode))."
}

Write-Host "HemoPet aberto com sucesso. Banco local: 127.0.0.1:$mysqlPort, usuario root, sem senha." -ForegroundColor Green

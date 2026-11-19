[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$previousDbUrl = $env:HEMOPET_DB_URL
$previousDbUser = $env:HEMOPET_DB_USER
$previousDbPassword = $env:HEMOPET_DB_PASSWORD
$testSchema = 'hemopet_test_' + [Guid]::NewGuid().ToString('N')
$testCreated = $false

try {
    . (Join-Path $PSScriptRoot 'iniciar-hemopet.ps1') -Preparar
    $testScriptDir = Join-Path $runtimeRoot $testSchema
    New-Item -ItemType Directory -Path $testScriptDir -Force | Out-Null
    $allScripts = @('database/01_criacao_tabelas_hemopet.sql', 'database/02_insercao_dados.sql') + $updates

    Write-Step "Preparando banco de teste isolado $testSchema..."
    $testCreated = $true
    foreach ($sqlScript in $allScripts) {
        $sql = [IO.File]::ReadAllText((Join-Path $projectRoot $sqlScript))
        $sql = [regex]::Replace($sql, '\bhemopet\b', $testSchema)
        $testSqlPath = Join-Path $testScriptDir ([IO.Path]::GetFileName($sqlScript))
        [IO.File]::WriteAllText($testSqlPath, $sql, [Text.UTF8Encoding]::new($false))
        $sqlClientPath = $testSqlPath.Replace('\', '/')
        & $mysqlExe @clientArgs --execute="source $sqlClientPath" | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Falha ao preparar $sqlScript no banco de teste." }
    }

    $testClasses = Join-Path $runtimeRoot 'test-classes'
    New-Item -ItemType Directory -Path $testClasses -Force | Out-Null
    $testSources = (Get-ChildItem (Join-Path $projectRoot 'src\test\java') -Filter '*.java' -Recurse -File).FullName
    & $javac --release 17 -encoding UTF-8 -cp "$classesDir;$libDir\*" -d $testClasses $testSources
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar os testes.' }

    $env:HEMOPET_DB_URL = $env:HEMOPET_DB_URL.Replace('/hemopet?', "/$testSchema`?")
    $testClasspath = "$testClasses;$classpath"
    $java = Find-JavaTool 'java'
    foreach ($testMain in @('OfflineInterfaceCheck', 'DatabaseSmokeCheck', 'Etapa06IntegrationCheck')) {
        & $java '-Djava.awt.headless=true' -cp $testClasspath "br.com.hemopet.app.$testMain"
        if ($LASTEXITCODE -ne 0) { throw "Falha em $testMain." }
    }
    Write-Host 'Etapa 06: todas as verificacoes passaram.' -ForegroundColor Green
} finally {
    if ($testCreated -and $testSchema -match '^hemopet_test_[a-f0-9]{32}$' -and $mysqlExe) {
        & $mysqlExe @clientArgs --execute="DROP DATABASE IF EXISTS $testSchema;"
    }
    $env:HEMOPET_DB_URL = $previousDbUrl
    $env:HEMOPET_DB_USER = $previousDbUser
    $env:HEMOPET_DB_PASSWORD = $previousDbPassword
}

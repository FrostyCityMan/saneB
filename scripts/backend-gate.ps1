param(
    [switch]$SkipTests,
    [switch]$SkipSmokeRun,
    [switch]$SkipFlywayDocker,
    [string]$PostgresImage = 'postgres:15'
)

$ErrorActionPreference = 'Stop'

$candidateHomes = @()

if ($env:SANEB_JAVA_HOME) {
    $candidateHomes += $env:SANEB_JAVA_HOME
}

if ($env:JAVA_HOME) {
    $candidateHomes += $env:JAVA_HOME
}

$codexJdks = Join-Path $HOME 'Documents\Codex\jdks'
if (Test-Path -LiteralPath $codexJdks) {
    $candidateHomes += Get-ChildItem -LiteralPath $codexJdks -Directory -Filter 'jdk-21*' |
        Sort-Object Name -Descending |
        ForEach-Object { $_.FullName }
}

function Test-Java21Home {
    param([string]$Path)

    if (-not $Path) {
        return $false
    }

    $javaExe = Join-Path $Path 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $javaExe)) {
        return $false
    }

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $versionOutput = & $javaExe -version 2>&1 | Out-String
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    return $versionOutput -match 'version "21\.'
}

$javaHome = $candidateHomes |
    Where-Object { Test-Java21Home $_ } |
    Select-Object -First 1

if (-not $javaHome) {
    throw 'Java 21 JDK was not found. Set SANEB_JAVA_HOME or JAVA_HOME to a Java 21 JDK path.'
}

$env:JAVA_HOME = $javaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

if ($env:GRADLE_OPTS) {
    if ($env:GRADLE_OPTS -notmatch 'javax\.net\.ssl\.trustStoreType=Windows-ROOT') {
        $env:GRADLE_OPTS = "$env:GRADLE_OPTS -Djavax.net.ssl.trustStoreType=Windows-ROOT"
    }
} else {
    $env:GRADLE_OPTS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT'
}

Write-Host "JAVA_HOME=$env:JAVA_HOME"
java -version

$repoDockerConfig = Join-Path (Get-Location) 'build\docker-config'
New-Item -ItemType Directory -Force -Path $repoDockerConfig | Out-Null
$repoDockerConfigFile = Join-Path $repoDockerConfig 'config.json'
if (-not (Test-Path -LiteralPath $repoDockerConfigFile)) {
    '{}' | Set-Content -LiteralPath $repoDockerConfigFile -Encoding ASCII
}

$previousDockerConfig = $env:DOCKER_CONFIG
$env:DOCKER_CONFIG = $repoDockerConfig
Write-Host "DOCKER_CONFIG=$env:DOCKER_CONFIG"

function Invoke-GateCommand {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock]$Command
    )

    $problemsReport = Join-Path (Get-Location) 'build\reports\problems\problems-report.html'
    if (Test-Path -LiteralPath $problemsReport) {
        Remove-Item -LiteralPath $problemsReport -Force
    }

    & $Command
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

if (-not $SkipTests) {
    Invoke-GateCommand { .\gradlew.bat cleanTest test --console=plain }
}

if (-not $SkipSmokeRun) {
    Invoke-GateCommand {
        .\gradlew.bat bootRun --console=plain --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.flyway.enabled=false"
    }
}

function Invoke-FlywayDockerGate {
    $containerName = 'saneb-flyway-gate'
    $databaseName = 'saneb_gate'
    $databaseUser = 'saneb_gate'
    $databasePassword = "saneb_$([Guid]::NewGuid().ToString('N'))"

    $previousDbUrl = $env:DB_URL
    $previousDbUsername = $env:DB_USERNAME
    $previousDbPassword = $env:DB_PASSWORD
    $previousFlywayIntegration = $env:SANEB_FLYWAY_INTEGRATION
    $previousAuthSmoke = $env:SANEB_AUTH_SMOKE
    $previousAnnouncementSmoke = $env:SANEB_ANNOUNCEMENT_SMOKE

    try {
        Invoke-GateCommand { docker version --format '{{.Server.Version}}' }

        $existingContainer = docker ps -a --filter "name=^/$containerName$" --format '{{.Names}}'
        if ($existingContainer -eq $containerName) {
            Invoke-GateCommand { docker rm -f $containerName }
        }

        Invoke-GateCommand {
            docker run `
                --name $containerName `
                -e POSTGRES_DB=$databaseName `
                -e POSTGRES_USER=$databaseUser `
                -e POSTGRES_PASSWORD=$databasePassword `
                -p 5432 `
                -d $PostgresImage
        }

        $isReady = $false
        for ($i = 0; $i -lt 60; $i++) {
            docker exec $containerName pg_isready -U $databaseUser -d $databaseName | Out-Null
            if ($LASTEXITCODE -eq 0) {
                $isReady = $true
                break
            }
            Start-Sleep -Seconds 1
        }

        if (-not $isReady) {
            throw 'PostgreSQL Docker container did not become ready within 60 seconds.'
        }

        $portLine = docker port $containerName 5432/tcp | Select-Object -First 1
        if ($portLine -notmatch ':(\d+)$') {
            throw "Cannot resolve PostgreSQL host port from docker port output: $portLine"
        }

        $hostPort = $Matches[1]
        $env:DB_URL = "jdbc:postgresql://localhost:$hostPort/$databaseName"
        $env:DB_USERNAME = $databaseUser
        $env:DB_PASSWORD = $databasePassword
        $env:SANEB_FLYWAY_INTEGRATION = 'true'

        Write-Host "DB_URL=jdbc:postgresql://localhost:$hostPort/$databaseName"
        Invoke-GateCommand { .\gradlew.bat flywayIntegrationTest --rerun-tasks --console=plain }

        $env:SANEB_ANNOUNCEMENT_SMOKE = 'true'
        Invoke-GateCommand { .\gradlew.bat announcementSmokeIntegrationTest --rerun-tasks --console=plain }

        $env:SANEB_AUTH_SMOKE = 'true'
        Invoke-GateCommand { .\gradlew.bat authSmokeIntegrationTest --rerun-tasks --console=plain }
    } finally {
        $env:DB_URL = $previousDbUrl
        $env:DB_USERNAME = $previousDbUsername
        $env:DB_PASSWORD = $previousDbPassword
        $env:SANEB_FLYWAY_INTEGRATION = $previousFlywayIntegration
        $env:SANEB_AUTH_SMOKE = $previousAuthSmoke
        $env:SANEB_ANNOUNCEMENT_SMOKE = $previousAnnouncementSmoke

        docker rm -f $containerName | Out-Null
    }
}

if (-not $SkipFlywayDocker) {
    try {
        Invoke-FlywayDockerGate
    } finally {
        $env:DOCKER_CONFIG = $previousDockerConfig
    }
} else {
    $env:DOCKER_CONFIG = $previousDockerConfig
}

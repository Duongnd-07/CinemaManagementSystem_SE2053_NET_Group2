param([switch]$Database)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    & mvn -o -q test-compile
    if ($LASTEXITCODE -ne 0) { throw 'Maven test compilation failed.' }
    $testClasspath = 'target/classes;target/test-classes;target/SE2053-SWP391-G2/WEB-INF/lib/*'
    $testArgs = @('-ea', '-cp', $testClasspath, 'vn.edu.fpt.service.OnlineSalesReportChecks')
    if ($Database) { $testArgs += '--db' }
    & java @testArgs
    if ($LASTEXITCODE -ne 0) { throw 'UC-37 checks failed.' }
} finally {
    Pop-Location
}

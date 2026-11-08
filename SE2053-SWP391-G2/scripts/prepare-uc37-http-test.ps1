param([string]$OutputRoot = 'D:/Ky9/SWP/outputs/uc37_20261106', [switch]$Refresh)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$tomcatHome = 'D:/Tools/tomcat/apache-tomcat-10.1.60'
$testBase = Join-Path $OutputRoot 'tomcat'
if (Test-Path -LiteralPath $testBase) {
    if (!$Refresh) { throw 'Use a fresh output directory or pass -Refresh for this isolated test server.' }
    $testPidFile = Join-Path $OutputRoot 'tomcat.pid'
    if (!(Test-Path -LiteralPath $testPidFile)) { throw 'Missing test process identity file.' }
    $testPid = [int](Get-Content -LiteralPath $testPidFile)
    $testProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$testPid"
    if ($testProcess) {
        if (!$testProcess.CommandLine.Replace('\','/').Contains($testBase.Replace('\','/'))) {
            throw 'Test process identity mismatch; refusing to stop another process.'
        }
        Stop-Process -Id $testPid -Force
    }
}
foreach ($directory in @('conf','logs','temp','work','webapps/cinema')) {
    New-Item -ItemType Directory -Path (Join-Path $testBase $directory) -Force | Out-Null
}
Get-ChildItem -LiteralPath (Join-Path $tomcatHome 'conf') -File | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $testBase 'conf')
}
[xml]$serverConfig = Get-Content -LiteralPath (Join-Path $testBase 'conf/server.xml')
$serverConfig.Server.port = '-1'
$serverConfig.Server.Service.Connector | Where-Object { $_.protocol -like '*HTTP*' } | ForEach-Object { $_.port = '18087' }
$serverConfig.Save((Join-Path $testBase 'conf/server.xml'))
Add-Type -AssemblyName System.IO.Compression.FileSystem
$war = Join-Path $projectRoot 'target/SE2053-SWP391-G2.war'
Push-Location (Join-Path $testBase 'webapps/cinema')
try {
    # jar works on both Windows PowerShell 5 and PowerShell 7; the bool ZipFile overload requires newer .NET.
    & jar xf $war
    if ($LASTEXITCODE -ne 0) { throw 'Cannot extract the test WAR.' }
} finally {
    Pop-Location
}
# Only the isolated test deployment omits scheduled jobs; do not send mail or change existing shifts during HTTP checks.
$listenerRoot = Join-Path $testBase 'webapps/cinema/WEB-INF/classes/vn/edu/fpt/listener'
foreach ($listener in @('SeatHoldCleanupListener','SeatHoldExpiryListener','ShowtimeReminderListener','MovieStatusSyncListener','ShiftAutoCloseListener')) {
    $listenerFile = Join-Path $listenerRoot ($listener + '.class')
    if (Test-Path -LiteralPath $listenerFile) { Remove-Item -LiteralPath $listenerFile }
}
$javaExecutable = (Get-Command java).Source
$javaArguments = @('-Dcatalina.base=' + $testBase, '-Dcatalina.home=' + $tomcatHome,
    '-Djava.io.tmpdir=' + (Join-Path $testBase 'temp'), '-cp',
    ($tomcatHome + '/bin/bootstrap.jar;' + $tomcatHome + '/bin/tomcat-juli.jar'),
    'org.apache.catalina.startup.Bootstrap', 'start')
$serverProcess = Start-Process -FilePath $javaExecutable -ArgumentList $javaArguments -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $testBase 'logs/stdout.log') -RedirectStandardError (Join-Path $testBase 'logs/stderr.log')
$serverProcess.Id | Set-Content -LiteralPath (Join-Path $OutputRoot 'tomcat.pid')
Write-Output ('Test Tomcat PID ' + $serverProcess.Id + ', URL http://localhost:18087/cinema')

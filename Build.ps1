param([string]$JavaHome=$env:JAVA_HOME,[string]$AndroidHome=$env:ANDROID_HOME,[switch]$Offline)
$ErrorActionPreference='Stop'
if(-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome 'bin/java.exe'))){throw 'Set JAVA_HOME to a Java 17 installation or pass -JavaHome.'}
if(-not $AndroidHome -or -not (Test-Path $AndroidHome)){throw 'Set ANDROID_HOME to an Android SDK or pass -AndroidHome.'}
$env:JAVA_HOME=$JavaHome
$env:ANDROID_HOME=$AndroidHome
Push-Location $PSScriptRoot
try {
    $flags=@('--no-daemon','--max-workers=2','--console=plain')
    if($Offline){$flags+='--offline'}
    & .\gradlew.bat @flags testReleaseUnitTest lintRelease assembleRelease assembleDebug
    if($LASTEXITCODE -ne 0){throw 'Build or verification failed. See Gradle output.'}
} finally {Pop-Location}

param(
 [Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9._-]+:[0-9]{1,5}$')][string]$Device,
 [Parameter(Mandatory=$true)][ValidateSet('Install','Launch','HomeSettings','Accessibility','StockHome')][string]$Action,
 [string]$Apk,[string]$Adb='adb')
$ErrorActionPreference='Stop'
function Invoke-Adb([string[]]$Arguments) {
 $result=& $Adb @Arguments 2>&1
 if($LASTEXITCODE -ne 0 -or ($result -join "`n") -match 'Failure|SecurityException|Permission Denial'){throw 'Device refused the request; stop and inspect locally.'}
 $result
}
# Does not enable debugging, grant permissions, select a default, or disable apps.
Invoke-Adb -Arguments @('connect',$Device)
switch($Action) {
 'Install' {if(-not $Apk -or -not (Test-Path -LiteralPath $Apk -PathType Leaf)){throw 'Pass an explicit signed APK path. Release APKs are unsigned.'}; Invoke-Adb -Arguments @('-s',$Device,'install','-r',(Resolve-Path -LiteralPath $Apk).Path)}
 'Launch' {Invoke-Adb -Arguments @('-s',$Device,'shell','am','start','-n','dev.gunnerstv.launcher/.MainActivity')}
 'HomeSettings' {Invoke-Adb -Arguments @('-s',$Device,'shell','am','start','-a','android.settings.HOME_SETTINGS')}
 'Accessibility' {Invoke-Adb -Arguments @('-s',$Device,'shell','am','start','-a','android.settings.ACCESSIBILITY_SETTINGS')}
 'StockHome' {Invoke-Adb -Arguments @('-s',$Device,'shell','monkey','-p','com.google.android.apps.tv.launcherx','-c','android.intent.category.LEANBACK_LAUNCHER','1')}
}

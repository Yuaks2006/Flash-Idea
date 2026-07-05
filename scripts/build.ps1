$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$devTools = "G:\Agent_Workspace\DevTools"
$junction = "C:\FlashIdeaIntegrated"

$env:JAVA_HOME = Join-Path $devTools "Java\jdk-17.0.14+7"
$env:ANDROID_HOME = Join-Path $devTools "Android"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

$buildRoot = $projectRoot
if ($projectRoot -match "[^\x00-\x7F]") {
    if (Test-Path $junction) {
        $target = [string](Get-Item $junction).Target
        if ([IO.Path]::GetFullPath($target).TrimEnd("\") -ne $projectRoot.TrimEnd("\")) {
            throw "$junction already points to a different project: $target"
        }
    } else {
        cmd /c "mklink /J `"$junction`" `"$projectRoot`""
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to create build junction."
        }
    }
    $buildRoot = $junction
}

$asciiGradleHome = Join-Path $buildRoot ".gradle-ascii"
$userGradleHome = Join-Path $env:USERPROFILE ".gradle"
if (-not (Test-Path $asciiGradleHome)) {
    cmd /c "mklink /J `"$asciiGradleHome`" `"$userGradleHome`""
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create ASCII Gradle cache junction."
    }
}
$env:GRADLE_USER_HOME = $asciiGradleHome
New-Item -ItemType Directory -Path (Join-Path $buildRoot ".gradle-tmp") -Force | Out-Null

Push-Location $buildRoot
try {
    .\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --no-daemon --max-workers=1 --offline
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle verification failed."
    }
} finally {
    Pop-Location
}

Write-Host "APK: $buildRoot\app\build\outputs\apk\debug\app-debug.apk"

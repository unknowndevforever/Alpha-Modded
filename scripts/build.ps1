# Full build on Windows: original jar -> patched jar in dist\recreatecraft_patched.jar
# Usage: powershell -ExecutionPolicy Bypass -File scripts\build.ps1 [-Jar <original a1.1.2_01.jar>]
param(
    [string]$Jar = "$env:APPDATA\ModrinthApp\meta\versions\a1.1.2_01\a1.1.2_01.jar",
    [string]$Asm = "$env:APPDATA\ModrinthApp\meta\libraries\org\ow2\asm\asm\9.8\asm-9.8.jar",
    [string]$Lwjgl = "$env:APPDATA\ModrinthApp\meta\libraries\org\lwjgl\lwjgl\lwjgl\2.9.0\lwjgl-2.9.0.jar"
)
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$w = Join-Path $root "work"
foreach ($d in "orig", "out", "build", "pbuild") { $p = Join-Path $w $d; if (Test-Path $p) { Remove-Item -Recurse -Force $p }; New-Item -ItemType Directory -Force $p | Out-Null }
New-Item -ItemType Directory -Force (Join-Path $root "dist") | Out-Null

Push-Location (Join-Path $w "orig"); jar xf $Jar; Pop-Location

# 1. new classes (Java 8 target, compiled against the original jar + real LWJGL 2)
$src = "PoweredRail", "Ext", "CreativeScreen", "Names", "WorldsScreen", "WorldSettingsScreen", "Inv", "HttpSocket", "SkinScreen", "NetFx", "WeatherFx", "PacketHurt", "PacketSound", "PacketWeather" | ForEach-Object { Join-Path $root "src\$_.java" }
javac --release 8 -Xlint:-options -nowarn -cp "$w\orig;$Lwjgl" -d "$w\build" @src
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

# 2. patcher
javac -cp $Asm -d "$w\pbuild" (Join-Path $root "src\Patch.java")
if ($LASTEXITCODE -ne 0) { throw "javac Patch failed" }
java -cp "$w\pbuild;$Asm" Patch "$w\orig" "$w\out" (Join-Path $root "textures")
if ($LASTEXITCODE -ne 0) { throw "Patch failed" }

# 3. package
Copy-Item "$w\build\*.class" "$w\out\"
Remove-Item -Recurse -Force "$w\out\META-INF"
"Manifest-Version: 1.0`r`nCreated-By: recreatecraft patch`r`n" | Out-File -Encoding ascii "$w\MANIFEST.MF"
$dist = Join-Path $root "dist\recreatecraft_patched.jar"
if (Test-Path $dist) { Remove-Item $dist }
jar cfm $dist "$w\MANIFEST.MF" -C "$w\out" .
Write-Host "built $dist"

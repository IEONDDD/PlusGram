param(
    [string]$CMake = "cmake",
    [string]$JdkHome = $env:JAVA_HOME,
    [switch]$Benchmark
)
$ErrorActionPreference = "Stop"
function Invoke-Checked([string]$Tool, [string[]]$Arguments) {
    & $Tool @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Tool failed with exit code $LASTEXITCODE" }
}
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$taskCrate = Join-Path $taskRoot "rust/plusgram-text"
$taskOutput = Join-Path $taskCrate "target"
$taskJavac = Join-Path $JdkHome "bin/javac.exe"
$taskJava = Join-Path $JdkHome "bin/java.exe"
$taskPreviousJavaHome = $env:JAVA_HOME
Push-Location $taskRoot
try {
    $env:JAVA_HOME = $JdkHome
    Invoke-Checked "cargo" @("fmt", "--manifest-path", "$taskCrate/Cargo.toml", "--check")
    Invoke-Checked "cargo" @("clippy", "--offline", "--manifest-path", "$taskCrate/Cargo.toml", "--all-targets", "--", "-D", "warnings")
    Invoke-Checked "cargo" @("test", "--offline", "--manifest-path", "$taskCrate/Cargo.toml")
    Invoke-Checked "cargo" @("build", "--offline", "--release", "--manifest-path", "$taskCrate/Cargo.toml")
    Invoke-Checked $CMake @("-S", $taskCrate, "-B", "$taskOutput/host", "-G", "Visual Studio 17 2022", "-A", "x64", "-DRUST_ARCHIVE=$taskOutput/release/plusgram_text.lib")
    Invoke-Checked $CMake @("--build", "$taskOutput/host", "--config", "Release")
    $taskClasses = Join-Path $taskOutput "java-test"
    New-Item -ItemType Directory -Path $taskClasses -Force | Out-Null
    [IO.File]::WriteAllText("$taskClasses/BuildConfig.java", "package org.telegram.messenger; public final class BuildConfig { public static final boolean RUST_TEXT_ENABLED = false; }")
    Invoke-Checked $taskJavac @("-encoding", "UTF-8", "-d", $taskClasses,
        "$taskClasses/BuildConfig.java",
        "$taskRoot/TMessagesProj/src/main/java/com/plusgram/app/ai/network/ReasoningFilter.java",
        "$taskRoot/tests/rust-text/ReasoningFilterParityTest.java",
        "$taskRoot/tests/rust-text/ReasoningFilterBenchmark.java")
    $taskLibrary = "$taskOutput/host/Release/plusgram_text_host.dll"
    Invoke-Checked $taskJava @("--enable-native-access=ALL-UNNAMED", "-Xcheck:jni", "-cp", $taskClasses, "com.plusgram.app.ai.network.ReasoningFilterParityTest", $taskLibrary)
    Invoke-Checked $taskJava @("-cp", $taskClasses, "com.plusgram.app.ai.network.ReasoningFilterParityTest", "--fallback")
    Copy-Item -LiteralPath $taskLibrary -Destination "$taskOutput/host/Release/plusgram_text.dll" -Force
    $taskProductionClasses = Join-Path $taskOutput "java-production-test"
    New-Item -ItemType Directory -Path $taskProductionClasses -Force | Out-Null
    [IO.File]::WriteAllText("$taskProductionClasses/BuildConfig.java", "package org.telegram.messenger; public final class BuildConfig { public static final boolean RUST_TEXT_ENABLED = true; }")
    Invoke-Checked $taskJavac @("-encoding", "UTF-8", "-d", $taskProductionClasses,
        "$taskProductionClasses/BuildConfig.java",
        "$taskRoot/TMessagesProj/src/main/java/com/plusgram/app/ai/network/ReasoningFilter.java",
        "$taskRoot/tests/rust-text/ReasoningFilterParityTest.java")
    Invoke-Checked $taskJava @("--enable-native-access=ALL-UNNAMED", "-Xcheck:jni", "-Djava.library.path=$taskOutput/host/Release", "-cp", $taskProductionClasses, "com.plusgram.app.ai.network.ReasoningFilterParityTest", "--production")
    if ($Benchmark) {
        Invoke-Checked $taskJava @("--enable-native-access=ALL-UNNAMED", "-cp", $taskClasses, "com.plusgram.app.ai.network.ReasoningFilterBenchmark", $taskLibrary)
    }
} finally {
    $env:JAVA_HOME = $taskPreviousJavaHome
    Pop-Location
}

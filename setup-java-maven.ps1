# Java和Maven环境配置脚本
# 使用方法：以管理员身份运行此脚本可配置系统级环境变量
# 或者直接运行可配置用户级环境变量

Write-Host "正在配置Java和Maven环境..." -ForegroundColor Green

# 配置JAVA_HOME
$javaHome = "D:\jdk21"
if (Test-Path $javaHome) {
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "User")
    Write-Host "✓ JAVA_HOME已设置为: $javaHome" -ForegroundColor Green
} else {
    Write-Host "✗ 错误: Java目录不存在: $javaHome" -ForegroundColor Red
    exit 1
}

# 配置MAVEN_HOME
$mavenHome = "D:\apache-maven-3.5.4\apache-maven-3.5.4"
if (Test-Path $mavenHome) {
    [Environment]::SetEnvironmentVariable("MAVEN_HOME", $mavenHome, "User")
    Write-Host "✓ MAVEN_HOME已设置为: $mavenHome" -ForegroundColor Green
} else {
    Write-Host "✗ 错误: Maven目录不存在: $mavenHome" -ForegroundColor Red
    exit 1
}

# 更新PATH环境变量
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
$javaBin = "$javaHome\bin"
$mavenBin = "$mavenHome\bin"

$pathUpdated = $false
$newPath = $currentPath

# 添加Java bin目录
if ($newPath -notlike "*$javaBin*") {
    $newPath = "$newPath;$javaBin"
    $pathUpdated = $true
    Write-Host "✓ 已将Java bin目录添加到PATH" -ForegroundColor Green
}

# 添加Maven bin目录
if ($newPath -notlike "*$mavenBin*") {
    $newPath = "$newPath;$mavenBin"
    $pathUpdated = $true
    Write-Host "✓ 已将Maven bin目录添加到PATH" -ForegroundColor Green
}

if ($pathUpdated) {
    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
}

# 刷新当前会话的环境变量
$env:JAVA_HOME = $javaHome
$env:MAVEN_HOME = $mavenHome
$env:Path = "$javaBin;$mavenBin;" + $env:Path

Write-Host "`n配置完成！" -ForegroundColor Green
Write-Host "`n验证配置:" -ForegroundColor Yellow
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "MAVEN_HOME: $env:MAVEN_HOME"
Write-Host "`nJava版本:" -ForegroundColor Yellow
java -version
Write-Host "`nMaven版本:" -ForegroundColor Yellow
mvn -version

Write-Host "`n注意: 如果这是新配置，请重新打开终端窗口以使环境变量生效。" -ForegroundColor Cyan

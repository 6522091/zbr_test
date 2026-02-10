# 性能测试运行脚本 (PowerShell)
# 用于快速运行性能测试套件

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "GitHub Action Scheduler 性能测试" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# 设置Java环境
$env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "D:\jdk21" }
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 检查Java版本
Write-Host "检查Java版本..." -ForegroundColor Yellow
java -version
Write-Host ""

# 运行性能测试
$testType = $args[0]

switch ($testType) {
    "all" {
        Write-Host "运行所有性能测试..." -ForegroundColor Green
        mvn test -Dtest="*Performance*,*LoadTest*,*StressTest*"
    }
    "suite" {
        Write-Host "运行综合性能测试套件..." -ForegroundColor Green
        mvn test -Dtest=PerformanceTestSuite
    }
    "load" {
        Write-Host "运行负载测试..." -ForegroundColor Green
        mvn test -Dtest=LoadTest
    }
    "stress" {
        Write-Host "运行压力测试..." -ForegroundColor Green
        mvn test -Dtest=StressTest
    }
    "test" {
        if ($args[1]) {
            Write-Host "运行特定测试: $($args[1])" -ForegroundColor Green
            mvn test -Dtest=$args[1]
        } else {
            Write-Host "请指定测试名称" -ForegroundColor Red
        }
    }
    default {
        Write-Host "运行所有性能测试..." -ForegroundColor Green
        mvn test -Dtest="*Performance*,*LoadTest*,*StressTest*"
    }
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "性能测试完成" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

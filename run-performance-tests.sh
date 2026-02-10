#!/bin/bash

# 性能测试运行脚本
# 用于快速运行性能测试套件

echo "=========================================="
echo "GitHub Action Scheduler 性能测试"
echo "=========================================="
echo ""

# 设置Java环境
export JAVA_HOME=${JAVA_HOME:-"D:/jdk21"}
export PATH="$JAVA_HOME/bin:$PATH"

# 检查Java版本
echo "检查Java版本..."
java -version
echo ""

# 运行性能测试
echo "开始运行性能测试..."
echo ""

# 选项1: 运行所有性能测试
if [ "$1" == "all" ] || [ -z "$1" ]; then
    echo "运行所有性能测试..."
    mvn test -Dtest="*Performance*,*LoadTest*,*StressTest*"
fi

# 选项2: 运行综合性能测试
if [ "$1" == "suite" ]; then
    echo "运行综合性能测试套件..."
    mvn test -Dtest=PerformanceTestSuite
fi

# 选项3: 运行负载测试
if [ "$1" == "load" ]; then
    echo "运行负载测试..."
    mvn test -Dtest=LoadTest
fi

# 选项4: 运行压力测试
if [ "$1" == "stress" ]; then
    echo "运行压力测试..."
    mvn test -Dtest=StressTest
fi

# 选项5: 运行特定测试
if [ "$1" == "test" ] && [ -n "$2" ]; then
    echo "运行特定测试: $2"
    mvn test -Dtest="$2"
fi

echo ""
echo "=========================================="
echo "性能测试完成"
echo "=========================================="

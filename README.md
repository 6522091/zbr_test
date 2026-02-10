# GitHub Action Scheduler

一个支持GitHub Action解析和调度的Spring Boot应用，模拟GitHub Actions的运行环境。

## 项目概述

本项目是一个基于Spring Boot 3.x和Java 21的响应式Web应用，用于解析和调度GitHub Action配置。项目使用WebFlux实现响应式编程，并充分利用Java 21的虚拟线程特性来处理高并发场景。

## 技术栈

- **Java 21** - 使用虚拟线程特性
- **Spring Boot 3.2.0** - 应用框架
- **Spring WebFlux** - 响应式Web框架
- **Maven** - 项目构建工具
- **JUnit 5** - 测试框架
- **Lombok** - 简化代码

## 核心功能

### 1. GitHub Action解析与调度
- 提供`/api/v1/run`接口，接收GitHub Action JSON配置
- 解析Action配置，提取Job和Step信息
- 管理上下文传递
- 模拟向下游资源服务申请Runner
- 模拟Runner连接和执行过程

### 2. Runner管理
- **申请Runner**: `POST /api/v1/runners/allocate`
- **查询Runner状态**: `GET /api/v1/runners/{id}`
- **释放Runner**: `DELETE /api/v1/runners/{id}`
- 模拟Runner生命周期管理（ALLOCATED → CONNECTED → RUNNING → COMPLETED）

## 项目结构

```
scheduler_test/
├── src/
│   ├── main/
│   │   ├── java/com/scheduler/
│   │   │   ├── action/          # Action解析模块
│   │   │   ├── config/          # 配置类
│   │   │   ├── controller/      # 控制器层
│   │   │   ├── model/           # 数据模型
│   │   │   ├── runner/          # Runner管理模块
│   │   │   └── service/         # 业务逻辑层
│   │   └── resources/
│   │       └── application.yml  # 应用配置
│   └── test/
│       └── java/com/scheduler/
│           ├── controller/      # 控制器测试
│           ├── integration/     # 集成测试
│           └── service/         # 服务测试
├── .github/
│   └── workflows/
│       └── ci.yml              # CI/CD配置
└── pom.xml
```

## 快速开始

### 前置要求

- JDK 21
- Maven 3.5+

### 运行项目

1. **克隆项目**
```bash
git clone <repository-url>
cd scheduler_test
```

2. **编译项目**
```bash
mvn clean compile
```

3. **运行测试**
```bash
mvn test
```

4. **启动应用**
```bash
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动

### API使用示例

#### 1. 运行Action

```bash
curl -X POST http://localhost:8080/api/v1/run \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Action",
    "jobs": {
      "build": {
        "runs-on": "ubuntu-latest",
        "steps": [
          {
            "name": "Checkout",
            "uses": "actions/checkout@v3"
          },
          {
            "name": "Build",
            "run": "mvn clean package"
          }
        ]
      }
    }
  }'
```

#### 2. 申请Runner

```bash
curl -X POST http://localhost:8080/api/v1/runners/allocate \
  -H "Content-Type: application/json" \
  -d '{
    "runsOn": "ubuntu-latest",
    "runId": "run-12345678"
  }'
```

#### 3. 查询Runner状态

```bash
curl http://localhost:8080/api/v1/runners/{runnerId}
```

#### 4. 释放Runner

```bash
curl -X DELETE http://localhost:8080/api/v1/runners/{runnerId}
```

## 虚拟线程特性

项目充分利用Java 21的虚拟线程特性：

1. **VirtualThreadConfig**: 配置虚拟线程执行器
2. **RunnerService**: 使用虚拟线程处理Runner操作
3. **高并发支持**: 可以轻松处理数千个并发请求

虚拟线程的优势：
- 轻量级：可以创建数百万个虚拟线程
- 高并发：适合I/O密集型操作
- 简化编程：无需复杂的线程池管理

## 测试

### 运行所有测试
```bash
mvn test
```

### 运行特定测试
```bash
mvn test -Dtest=SchedulerIntegrationTest
```

### 测试覆盖
- 单元测试：控制器、服务层
- 集成测试：完整的Action调度流程
- 并发测试：虚拟线程性能测试

### 性能测试

项目包含完整的性能测试套件，位于 `src/test/java/com/scheduler/performance/`：

#### 性能测试套件

1. **PerformanceTestSuite** - 综合性能测试
   - 吞吐量测试（1000请求/60秒）
   - 响应时间测试（P50/P95/P99）
   - 高并发压力测试（1000并发）
   - 虚拟线程性能测试（10000请求）
   - 长时间稳定性测试（5分钟持续负载）
   - 资源使用测试

2. **LoadTest** - 负载测试
   - 轻负载（10并发，100请求）
   - 中等负载（50并发，500请求）
   - 重负载（100并发，1000请求）

3. **StressTest** - 压力测试
   - 突发流量测试
   - 逐步增加负载测试
   - 系统恢复测试

#### 运行性能测试

**使用Maven:**
```bash
# 运行所有性能测试
mvn test -Dtest="*Performance*,*LoadTest*,*StressTest*"

# 运行综合性能测试
mvn test -Dtest=PerformanceTestSuite

# 运行负载测试
mvn test -Dtest=LoadTest

# 运行压力测试
mvn test -Dtest=StressTest
```

**使用脚本 (Windows):**
```powershell
.\run-performance-tests.ps1 all
.\run-performance-tests.ps1 suite
.\run-performance-tests.ps1 load
.\run-performance-tests.ps1 stress
```

**使用脚本 (Linux/Mac):**
```bash
chmod +x run-performance-tests.sh
./run-performance-tests.sh all
./run-performance-tests.sh suite
./run-performance-tests.sh load
./run-performance-tests.sh stress
```

#### 性能基准

| 测试场景 | 并发数 | 吞吐量 | P95响应时间 | 成功率 |
|---------|--------|--------|-------------|--------|
| 轻负载 | 10 | >50 req/s | <2s | >99% |
| 中等负载 | 50 | >30 req/s | <5s | >95% |
| 重负载 | 100 | >20 req/s | <10s | >90% |
| 高并发 | 1000 | >10 req/s | <30s | >80% |

详细性能测试文档请参考: `src/test/java/com/scheduler/performance/README.md`

## CI/CD

项目配置了GitHub Actions工作流，自动执行：
- 代码编译
- 单元测试
- 集成测试
- 打包应用

工作流文件：`.github/workflows/ci.yml`

## 配置说明

主要配置在 `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

scheduler:
  runner:
    allocation-timeout: 5000
    connection-timeout: 10000
    execution-timeout: 30000
```

## 开发指南

### 添加新的功能

1. **添加新的Controller**
   - 在 `controller` 包下创建新的控制器
   - 使用 `@RestController` 和 `@RequestMapping` 注解

2. **添加新的Service**
   - 在 `service` 包下创建服务类
   - 使用 `@Service` 注解
   - 使用响应式编程（Mono/Flux）

3. **添加测试**
   - 在对应的测试包下创建测试类
   - 使用 `@SpringBootTest` 进行集成测试
   - 使用 `@WebFluxTest` 进行控制器测试

## 性能优化

- 使用响应式编程避免阻塞
- 虚拟线程处理高并发
- 异步处理Runner操作
- 合理的超时配置

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request！

## 作者

GitHub Action Scheduler Team

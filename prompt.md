请帮我实现如下功能
1.一个支持github action的maven项目，项目名称为scheduler_test，项目结构如下：
    - src
    - test
    - pom.xml
    - .github
    - .github/workflows
    - .github/workflows/main.yml
    - .github/workflows/main.yml    
2.实现功能1：
支持GitHub action解析并调度
通过run接口，接受一个json的GitHub action，服务器将完成解析、上下文传递，并向下游资源服务申请runner，模拟runner连接上来后调度执行
项目请使用springboot，接口使用响应式接口。
由于我这里没有实际的下游资源服务，请模拟一个下游资源服务，并实现一个简单的下游资源服务接口，接口如下：
runner的申请 查询 释放
请提供junit的测试代码。
并能通过简单的aciton模拟调度
考虑多并发场景，请优先使用java21的轻量级线程特性
同时请实现相关github action的ci文件，便于后续上传github




我来帮您整理和优化这个需求，使其更清晰、结构化：

## 优化后的需求说明

### 项目概述
开发一个支持GitHub Action解析和调度的Spring Boot应用，模拟GitHub Actions的运行环境。

### 核心功能要求

#### 1. 项目结构
```
scheduler_test/
├── src/
│   ├── main/java/com/scheduler/
│   └── test/java/com/scheduler/
├── .github/
│   └── workflows/
│       └── main.yml
└── pom.xml
```

#### 2. 主要功能模块

**功能1：GitHub Action解析与调度**
- 提供`/run`接口（响应式），接收GitHub Action JSON配置
- 解析Action配置，管理上下文传递
- 模拟向下游资源服务申请Runner
- 模拟Runner连接和执行过程

**功能2：模拟下游资源服务**
- Runner申请接口
- Runner状态查询接口  
- Runner释放接口
- 模拟Runner生命周期管理

#### 3. 技术要求
- Spring Boot 3.x + WebFlux（响应式）
- Java 21（使用虚拟线程特性）
- Maven项目
- JUnit 5测试
- 支持并发场景

#### 4. 测试要求
- 完整的单元测试
- 集成测试
- 并发场景测试
- 简单的Action调度演示

#### 5. CI/CD配置
- GitHub Actions工作流配置
- 自动化构建和测试

---

## 项目实现建议

### 1. 项目架构设计
```java
// 建议的包结构
com.scheduler
├── controller        // 控制器层
├── service           // 业务逻辑层  
├── model             // 数据模型
├── repository        // 数据访问层
├── config            // 配置类
├── runner            // Runner管理模块
└── action            // Action解析模块
```

### 2. 核心接口设计

```java
// 主调度接口
POST /api/v1/run
Content-Type: application/json
Request: GitHub Action JSON
Response: 调度结果

// Runner管理接口
POST /api/v1/runners/allocate   // 申请Runner
GET  /api/v1/runners/{id}       // 查询Runner状态  
DELETE /api/v1/runners/{id}     // 释放Runner
```

### 3. 关键技术点

**Java 21虚拟线程使用：**
```java
// 使用虚拟线程处理并发请求
@Configuration
public class VirtualThreadConfig {
    @Bean
    public TaskExecutor taskExecutor() {
        return new TaskExecutorAdapter(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
```

**响应式编程：**
```java
@RestController
@RequestMapping("/api/v1")
public class SchedulerController {
    
    @PostMapping("/run")
    public Mono<RunResponse> runAction(@RequestBody ActionRequest request) {
        return actionService.parseAndSchedule(request);
    }
}
```

### 4. 测试策略

```java
@SpringBootTest
@AutoConfigureWebTestClient
class SchedulerIntegrationTest {
    
    @Test
    @VirtualThreadOnly
    void testConcurrentActionExecution() {
        // 并发测试虚拟线程性能
    }
}
```

### 5. GitHub Actions CI配置示例

```yaml
name: Java CI with Maven

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Build with Maven
      run: mvn clean compile
      
    - name: Run tests
      run: mvn test
      
    - name: Package application
      run: mvn package -DskipTests
```

### 6. 实施建议

1. **第一阶段：基础框架搭建**
   - 创建Spring Boot项目
   - 配置Java 21和依赖
   - 实现基础响应式接口

2. **第二阶段：核心功能实现**
   - Action解析器
   - Runner模拟服务
   - 调度引擎

3. **第三阶段：并发优化**
   - 虚拟线程集成
   - 并发控制
   - 性能测试

4. **第四阶段：测试和CI**
   - 编写测试用例
   - 配置GitHub Actions
   - 文档完善


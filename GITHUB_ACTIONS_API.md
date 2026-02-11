# GitHub Actions 状态查询 API

这个模块提供了实时获取 GitHub Actions 工作流运行状态的功能。

## 功能特性

- 获取指定仓库的工作流运行列表
- 获取特定工作流运行的详细状态
- 实时监控工作流运行状态（通过 Server-Sent Events）

## API 接口

### 1. 获取工作流运行列表

**请求:**
```
GET /api/v1/github-actions/{owner}/{repo}/runs?perPage=30
```

**参数:**
- `owner`: 仓库所有者
- `repo`: 仓库名称
- `perPage`: 每页数量（可选，默认 30，最大 100）

**响应示例:**
```json
{
  "totalCount": 100,
  "workflowRuns": [
    {
      "id": 123456789,
      "name": "CI",
      "runNumber": 42,
      "status": "completed",
      "conclusion": "success",
      "event": "push",
      "headBranch": "main",
      "headSha": "abc123def456",
      "htmlUrl": "https://github.com/owner/repo/actions/runs/123456789",
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:05:00"
    }
  ]
}
```

### 2. 获取工作流运行状态

**请求:**
```
GET /api/v1/github-actions/{owner}/{repo}/runs/{runId}
```

**参数:**
- `owner`: 仓库所有者
- `repo`: 仓库名称
- `runId`: 工作流运行 ID

**响应示例:**
```json
{
  "id": 123456789,
  "name": "CI",
  "runNumber": 42,
  "status": "in_progress",
  "conclusion": null,
  "event": "push",
  "headBranch": "feature-branch",
  "headSha": "abc123def456",
  "htmlUrl": "https://github.com/owner/repo/actions/runs/123456789",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:05:00"
}
```

### 3. 实时监控工作流运行状态（SSE）

**请求:**
```
GET /api/v1/github-actions/{owner}/{repo}/runs/{runId}/watch?interval=5
```

**参数:**
- `owner`: 仓库所有者
- `repo`: 仓库名称
- `runId`: 工作流运行 ID
- `interval`: 轮询间隔（秒，可选，默认 5）

**响应:**
返回 Server-Sent Events 流，实时推送工作流状态更新，直到工作流完成。

**使用示例（JavaScript）:**
```javascript
const eventSource = new EventSource(
  '/api/v1/github-actions/owner/repo/runs/123456789/watch?interval=5'
);

eventSource.onmessage = (event) => {
  const status = JSON.parse(event.data);
  console.log('Workflow status:', status.status);
  console.log('Conclusion:', status.conclusion);

  if (status.status === 'completed') {
    eventSource.close();
  }
};

eventSource.onerror = (error) => {
  console.error('Error:', error);
  eventSource.close();
};
```

## 状态说明

### status 字段
- `queued`: 排队中
- `in_progress`: 执行中
- `completed`: 已完成

### conclusion 字段（仅在 status 为 completed 时有值）
- `success`: 成功
- `failure`: 失败
- `cancelled`: 已取消
- `skipped`: 已跳过
- `timed_out`: 超时
- `action_required`: 需要操作
- `neutral`: 中性

## 配置

### GitHub API Token（可选）

为了提高 API 速率限制，可以配置 GitHub Personal Access Token：

**方式 1: 环境变量**
```bash
export GITHUB_TOKEN=your_github_token_here
```

**方式 2: application.yml**
```yaml
github:
  api:
    token: your_github_token_here
```

### 创建 GitHub Token

1. 访问 GitHub Settings -> Developer settings -> Personal access tokens
2. 生成新 token，选择以下权限：
   - `repo` (如果需要访问私有仓库)
   - `workflow` (读取工作流信息)
3. 复制 token 并设置到环境变量或配置文件

**注意:** 如果不设置 token，API 将使用未认证请求，速率限制为每小时 60 次。设置 token 后，速率限制提升至每小时 5000 次。

## 测试

运行测试：
```bash
mvn test -Dtest=GitHubActionsControllerTest
```

## 技术实现

- 使用 Spring WebFlux 的 `WebClient` 调用 GitHub API
- 支持响应式编程模型（Mono/Flux）
- 实时监控通过 Server-Sent Events (SSE) 实现
- 使用轮询机制定期获取状态更新
- 状态去重，仅在状态变化时推送更新
- 自动结束监控（当工作流状态为 completed 时）

## 示例用法

### 使用 curl 测试

**获取工作流列表:**
```bash
curl http://localhost:8080/api/v1/github-actions/6522091/zbr_test/runs
```

**获取特定运行状态:**
```bash
curl http://localhost:8080/api/v1/github-actions/6522091/zbr_test/runs/123456789
```

**实时监控（SSE）:**
```bash
curl -N http://localhost:8080/api/v1/github-actions/6522091/zbr_test/runs/123456789/watch
```

## 依赖说明

本功能使用现有依赖，无需额外添加：
- Spring Boot WebFlux (已包含在 pom.xml)
- Jackson (用于 JSON 处理，已包含)
- Lombok (用于简化代码，已包含)

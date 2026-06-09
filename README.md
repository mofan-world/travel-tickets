# 出差车票管理系统

面向约 100 万注册用户的企业出差车票管理系统，覆盖车票归集、审批、风险识别、报销协同、统计汇总和全文检索。项目包含静态管理端原型、Java 后端、PostgreSQL 数据模型、OpenAPI 契约和架构设计文档。

## 技术栈

- 后端：Java 17、Spring Boot 3.2
- 数据库：PostgreSQL 16、Flyway
- 缓存：Redis 7
- 配置中心与服务发现：Nacos 2.3
- 搜索：Elasticsearch 8
- 前端原型：HTML、CSS、JavaScript

## 项目结构

```text
app/                 管理端静态原型
backend/             Spring Boot 后端服务
api/openapi.yaml     API 契约
database/schema.sql  面向生产的 PostgreSQL 数据模型
docs/architecture.md 百万用户规模架构设计
```

## 快速预览前端

直接用浏览器打开：

```text
app/index.html
```

## 后端运行

进入后端目录：

```powershell
cd backend
```

启动基础依赖：

```powershell
docker compose up -d postgres redis
```

轻量模式启动后端，适合本机低内存验证：

```powershell
.\run-local.cmd
```

完整平台模式启动后端，会启用 Nacos 和 Elasticsearch：

```powershell
docker compose up -d
.\run-platform.cmd
```

健康检查：

```powershell
curl http://localhost:8080/actuator/health
```

## 已实现接口

- `GET /api/v1/tickets`：车票分页列表
- `POST /api/v1/tickets`：创建车票
- `GET /api/v1/tickets/{ticketId}`：车票详情
- `POST /api/v1/approvals/tickets/{ticketId}/actions`：审批动作
- `GET /api/v1/risk/events`：风险事件
- `GET /api/v1/reports/summary`：仪表盘摘要
- `GET /api/v1/search/tickets`：Elasticsearch 车票搜索
- `POST /api/v1/search/tickets/reindex`：按租户重建 ES 索引

所有业务接口通过 `X-Tenant-Id` 请求头做租户隔离。

## Nacos 与 ES

Nacos 配置样例：

```text
backend/src/main/resources/nacos/travel-ticket-service.yaml
```

ES 索引名：

```text
travel-ticket-v1
```

搜索示例：

```powershell
curl "http://localhost:8080/api/v1/search/tickets?q=北京&page=0&size=20" `
  -H "X-Tenant-Id: 10001"
```

重建索引：

```powershell
curl -X POST "http://localhost:8080/api/v1/search/tickets/reindex" `
  -H "X-Tenant-Id: 10001"
```

## 容量假设

- 注册用户：1,000,000
- 月活用户：300,000
- 日活用户：80,000
- 月新增车票：3,000,000 到 8,000,000
- 写入峰值：2,500 TPS
- 读取峰值：15,000 QPS
- 核心接口 P95：300ms 内

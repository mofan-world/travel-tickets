# 出差车票管理系统

面向约 100 万注册用户的企业出差车票管理系统，覆盖车票归集、审批、补票、核销、风险识别、统计汇总和全文检索。项目包含 Vue 3 前端、Java Spring Boot 后端、PostgreSQL 数据模型、Redis 缓存、Nacos 配置与服务发现、Elasticsearch 搜索能力以及 OpenAPI 契约。

## 技术栈

- 后端：Java 17、Spring Boot 3.2
- 数据库：PostgreSQL 16、Flyway
- 缓存：Redis 7
- 配置中心与服务发现：Nacos 2.3
- 搜索：Elasticsearch 8
- 前端：Vue 3、HTML、CSS、Node.js 静态构建脚本

## 项目结构

```text
app/                 Vue 3 管理端前端
backend/             Spring Boot 后端服务
api/openapi.yaml     API 契约
database/schema.sql  面向生产的 PostgreSQL 数据模型
docs/architecture.md 百万用户规模架构设计
```

## 前端运行

前端使用随项目提交的 Vue 3 浏览器运行时，不需要联网安装 npm 依赖。

```powershell
cd app
npm run dev
```

默认地址：

```text
http://127.0.0.1:5173
```

构建与预览：

```powershell
npm run build
npm run preview
```

前端包含注册、登录、车票新增、编辑、删除、审批、驳回、补票、筛选、风险提示和 CSV 导出。演示账号：

```text
admin@travel.local / admin123
```

前端默认连接后端：

```text
http://127.0.0.1:8080
```

注册、登录、车票管理、审批、风险列表和 ES 重建索引都通过后端 API 完成，不再使用浏览器本地数据作为业务主存储。

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

## 前后端联动与数据写入

- PostgreSQL：用户、车票主数据通过 Flyway 建表并持久化。
- Redis：后端保留 Spring Cache，同时在车票新增、更新、审批、删除后维护热点快照。
- Elasticsearch：车票新增、更新、审批后写入 `travel-ticket-v1`；删除车票时同步删除对应索引文档。
- 前端：登录成功后保存会话中的 `tenantId`，后续请求统一带 `X-Tenant-Id` 请求头。

常用 Redis key：

```text
travel-ticket:user:{userId}
travel-ticket:ticket:{tenantId}:{ticketId}
travel-ticket:tenant:{tenantId}:tickets
```

## 容量假设

- 注册用户：1,000,000
- 月活用户：300,000
- 日活用户：80,000
- 月新增车票：3,000,000 到 8,000,000
- 写入峰值：2,500 TPS
- 读取峰值：15,000 QPS
- 核心接口 P95：200ms 内

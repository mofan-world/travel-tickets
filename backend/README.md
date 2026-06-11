# 出差车票管理系统后端

后端采用 Java 17、Spring Boot 3.2、PostgreSQL、Redis、Nacos 和 Elasticsearch。当前项目支持两种运行模式：

- 轻量本地模式：只启用 PostgreSQL 和 Redis，便于低内存机器验证核心业务。
- 完整平台模式：启用 PostgreSQL、Redis、Nacos 配置中心/服务发现和 Elasticsearch 搜索索引。

## 技术栈

- Spring Boot Web / Validation / Actuator
- Spring Data JPA + PostgreSQL
- Spring Cache + Redis
- Spring Cloud Alibaba Nacos Config / Discovery
- Spring Data Elasticsearch
- Flyway

## 启动依赖

```powershell
docker compose up -d
```

如果只想启动数据库和缓存：

```powershell
docker compose up -d postgres redis
```

## 启动后端

轻量本地模式：

```powershell
.\run-local.cmd
```

完整平台模式：

```powershell
.\run-platform.cmd
```

完整平台模式会先发布 Nacos 配置，然后启动应用。启动参数不再覆盖数据库、Redis 或 ES 地址，应用会从 Nacos Data ID 中读取这些运行配置。

完整平台模式会设置：

```text
NACOS_ENABLED=true
NACOS_SERVER_ADDR=127.0.0.1:8848
ES_ENABLED=true
ES_URIS=http://127.0.0.1:9200
```

## Nacos 配置

应用配置中心 Data ID：

```text
travel-ticket-service.yaml
```

配置样例在：

```text
src/main/resources/nacos/travel-ticket-service.yaml
```

发布到本地 Nacos：

```powershell
.\publish-nacos-config.cmd
```

也可以在 Nacos 控制台创建同名配置，并把样例内容导入。当前项目约定：

- `application.yml` 只保留应用名、Nacos Config 和 Nacos Discovery 连接信息。
- `travel-ticket-service.yaml` 保存端口、PostgreSQL、Redis、Elasticsearch、Flyway、JPA、Actuator 和日志配置。
- 完整平台模式启动时通过 `spring.config.import=optional:nacos:travel-ticket-service.yaml` 从 Nacos 获取运行配置。

应用本地启动配置：

```yaml
spring:
  application:
    name: travel-ticket-service
  config:
    import:
      - optional:nacos:travel-ticket-service.yaml
  cloud:
    nacos:
      discovery:
        enabled: ${NACOS_ENABLED:true}
      config:
        enabled: ${NACOS_ENABLED:true}
        refresh-enabled: true
```

## Elasticsearch 使用

应用会在创建车票、审批变更时写入 ES 索引：

```text
travel-ticket-v1
```

搜索接口：

```powershell
curl "http://localhost:8080/api/v1/search/tickets?q=北京&page=0&size=20" `
  -H "X-Tenant-Id: 10001"
```

如果 ES 是后续启用的，可以先把当前租户已有 PostgreSQL 数据重建到 ES：

```powershell
curl -X POST "http://localhost:8080/api/v1/search/tickets/reindex" `
  -H "X-Tenant-Id: 10001"
```

## 核心 API 示例

创建车票：

```powershell
curl -X POST http://localhost:8080/api/v1/tickets `
  -H "Content-Type: application/json" `
  -H "X-Tenant-Id: 10001" `
  -d '{
    "employeeId": 20001,
    "ticketNo": "G12-20260609-001",
    "travelType": "TRAIN",
    "departureCity": "上海",
    "arrivalCity": "北京",
    "carrierNo": "G12",
    "departAt": "2026-06-09T02:30:00Z",
    "amount": 553.00,
    "currency": "CNY"
  }'
```

审批通过：

```powershell
curl -X POST http://localhost:8080/api/v1/approvals/tickets/1/actions `
  -H "Content-Type: application/json" `
  -H "X-Tenant-Id: 10001" `
  -d '{"action": "approve", "comment": "符合差旅标准"}'
```

健康检查：

```powershell
curl http://localhost:8080/actuator/health
```

## IDEA 控制台日志

后端已配置 `logback-spring.xml`，在 IntelliJ IDEA 中直接运行 `TravelTicketApplication` 时，Run 控制台会输出启动日志和 `/api/**` 接口访问日志。

推荐 IDEA Run Configuration：

```text
Main class: com.codex.travel.ticket.TravelTicketApplication
JDK: 17
Working directory: backend
Environment variables:
NACOS_ENABLED=true;NACOS_SERVER_ADDR=127.0.0.1:8848;ES_ENABLED=true;ES_URIS=http://127.0.0.1:9200
```

接口日志示例：

```text
HTTP GET /api/v1/tickets?page=0&size=20 status=200 elapsed=35ms tenant=10001
```

操作日志和异常日志会同时写入 PostgreSQL：

```text
operation_logs
exception_logs
```

运维查询接口：

```powershell
curl "http://localhost:8080/api/v1/ops/logs/operations?page=0&size=20" `
  -H "X-Tenant-Id: 10001"

curl "http://localhost:8080/api/v1/ops/logs/exceptions?page=0&size=20" `
  -H "X-Tenant-Id: 10001"
```

# 新能源充电设施运行监测与智能告警平台

面向新能源充电设施运维场景的后端管理平台，实现设备台账管理、运行数据上报、实时状态监测、动态规则告警、实时推送和工单流转闭环。

## 核心业务流程

```text
设备运行数据上报
→ MySQL 历史数据保存 + Redis 实时状态更新
→ RabbitMQ 异步投递
→ 消费者从 Redis 缓存读取启用规则
→ THRESHOLD 阈值告警检测
→ CONTINUOUS 连续异常检测（Redis 滑动窗口）
→ alarm_record 告警记录生成
→ 事务提交后 WebSocket 实时推送
→ alarm_level=3 严重告警自动生成 work_order
→ 运维人员接单 → 处理 → 完成
```

## 技术栈

| 技术 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.3.13 | 后端框架 |
| JDK | 17 | 运行环境 |
| MyBatis-Plus | 3.5.16 | ORM / 分页 / 逻辑删除 |
| MySQL | 8.0+ | 关系数据库 |
| Redis | 6/7 | 规则缓存 / 滑动窗口 / 实时状态 / 心跳 |
| RabbitMQ | 3.x | 异步告警消息队列 |
| WebSocket | Spring 原生 | 告警实时推送 |
| Sa-Token | 1.45.0 | 登录认证 |
| Knife4j | 4.5.0 | 接口文档 |
| Hutool | 5.8.36 | 工具类 |

## 项目结构

```
charge-monitor-platform/
├── README.md
├── docs/
│   ├── 01-需求说明.md
│   ├── 02-系统架构设计.md
│   ├── 03-数据库设计.md
│   ├── 04-接口文档说明.md
│   ├── 05-告警规则设计.md
│   ├── 06-部署运行文档.md
│   ├── 07-接口测试指南.md
│   └── 08-项目亮点总结.md
├── sql/
│   ├── init.sql
│   └── work_order.sql
└── charge-monitor-backend/
    └── src/main/java/com/ccbcc/charge/monitor/
        ├── config/          # MyBatis-Plus / Redis / Sa-Token / RabbitMQ / WebSocket / Knife4j
        ├── common/          # 统一返回、异常处理、常量
        ├── module/
        │   ├── auth/        # 登录认证
        │   ├── device/      # 设备管理 + 数据上报
        │   ├── alarm/       # 告警检测 + 告警记录 + 规则管理 + MQ 消费 + WebSocket
        │   ├── report/      # 运行概览报表
        │   └── workorder/   # 工单管理 + 自动生成
        └── task/            # 设备离线检测定时任务
```

## 数据库表

| 表名 | 说明 |
|---|---|
| `sys_user` | 用户表 |
| `sys_role` | 角色表 |
| `sys_user_role` | 用户角色关联表 |
| `device_info` | 设备信息表 |
| `device_data` | 设备运行数据表 |
| `alarm_rule` | 告警规则表（动态配置） |
| `alarm_record` | 告警记录表 |
| `work_order` | 运维工单表 |

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6/7
- RabbitMQ 3.x

### 2. 基础服务启动

```bash
# MySQL（确保已启动）
# Redis
redis-server

# RabbitMQ（Docker 方式）
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3.13-management
```

### 3. 初始化数据库

在 IDEA 数据库工具或 Navicat 中执行 `sql/init.sql`，再执行 `sql/work_order.sql`。

### 4. 配置环境变量

```bash
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your_password
export REDIS_PASSWORD=           # Redis 无密码可留空
```

### 5. 启动项目

```bash
cd charge-monitor-backend
mvn spring-boot:run
```

或在 IDEA 中运行 `ChargeMonitorApplication.java`。

### 6. 验证

- Knife4j 接口文档：http://localhost:8080/doc.html
- 测试账号：`admin` / `123456`

## 接口概览

| 模块 | 路径 | 说明 |
|---|---|---|
| 认证 | `POST /api/auth/login` | 用户登录 |
| 设备 | `POST /api/device` | 新增设备 |
| 设备 | `GET /api/device/page` | 分页查询设备 |
| 设备 | `GET /api/device/{id}` | 查询设备详情 |
| 设备 | `PUT /api/device/{id}` | 修改设备 |
| 设备 | `DELETE /api/device/{id}` | 删除设备 |
| 数据 | `POST /api/device/data/report` | 上报运行数据 |
| 数据 | `GET /api/device/data/latest/{deviceCode}` | 查询最新状态 |
| 数据 | `GET /api/device/data/history` | 查询历史数据 |
| 告警 | `GET /api/alarm/record/page` | 分页查询告警 |
| 告警 | `GET /api/alarm/record/{id}` | 查询告警详情 |
| 告警 | `PUT /api/alarm/record/{id}/ack` | 确认告警 |
| 告警 | `PUT /api/alarm/record/{id}/recover` | 恢复告警 |
| 规则 | `POST /api/alarm/rule` | 新增告警规则 |
| 规则 | `GET /api/alarm/rule/page` | 分页查询规则 |
| 规则 | `GET /api/alarm/rule/{id}` | 查询规则详情 |
| 规则 | `PUT /api/alarm/rule/{id}` | 修改规则 |
| 规则 | `DELETE /api/alarm/rule/{id}` | 删除规则 |
| 规则 | `PUT /api/alarm/rule/{id}/enable` | 启用规则 |
| 规则 | `PUT /api/alarm/rule/{id}/disable` | 禁用规则 |
| 工单 | `GET /api/work-order/page` | 分页查询工单 |
| 工单 | `GET /api/work-order/{id}` | 查询工单详情 |
| 工单 | `PUT /api/work-order/{id}/accept` | 接单 |
| 工单 | `PUT /api/work-order/{id}/finish` | 完成工单 |
| 工单 | `PUT /api/work-order/{id}/close` | 关闭工单 |
| 工单 | `DELETE /api/work-order/{id}` | 删除工单 |
| 报表 | `GET /api/report/overview` | 今日运行概览 |

## WebSocket 测试

浏览器控制台连接：

```javascript
const ws = new WebSocket("ws://localhost:8080/ws/alarm");
ws.onmessage = (e) => console.log("收到推送:", JSON.parse(e.data));
```

## Redis Key 约定

| Key | 类型 | 说明 |
|---|---|---|
| `device:status:{deviceCode}` | String | 设备最新运行状态 |
| `device:heartbeat:{deviceCode}` | String | 设备最近心跳时间 |
| `device:online:set` | Set | 当前在线设备集合 |
| `device:alarm:set` | Set | 当前告警设备集合 |
| `alarm:rule:enabled:THRESHOLD` | String | 启用阈值规则缓存 |
| `alarm:continuous:window:{deviceCode}:{ruleCode}` | List | 连续异常滑动窗口 |

## 核心亮点

1. **RabbitMQ 异步告警** — 设备数据上报与告警检测解耦，降低接口阻塞风险
2. **动态告警规则配置** — alarm_rule 表 + 管理接口，运行时修改规则即时生效
3. **Redis 规则缓存** — Cache Aside 模式，规则变更后主动清缓存
4. **连续异常滑动窗口** — Redis List + LPUSH/LTRIM，窗口内命中次数达阈值才触发
5. **WebSocket 实时推送** — 告警生成后事务提交即推，广播模式
6. **自动工单生成** — 严重告警自动建单，uk_alarm_id 唯一索引防重复
7. **工单状态流转** — 待处理 → 处理中 → 已完成 / 已关闭，状态校验

## 项目文档

| 文档 | 内容 |
|---|---|
| [01-需求说明](docs/01-需求说明.md) | 项目背景、目标、功能范围 |
| [02-系统架构设计](docs/02-系统架构设计.md) | 架构设计、技术选型 |
| [03-数据库设计](docs/03-数据库设计.md) | 表结构、字段、索引 |
| [04-接口文档说明](docs/04-接口文档说明.md) | 全部接口参数与响应 |
| [05-告警规则设计](docs/05-告警规则设计.md) | 规则体系、去重策略 |
| [06-部署运行文档](docs/06-部署运行文档.md) | 环境要求、部署步骤 |
| [07-接口测试指南](docs/07-接口测试指南.md) | 完整测试流程 |
| [08-项目亮点总结](docs/08-项目亮点总结.md) | 技术亮点归纳 |

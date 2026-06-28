# 新能源充电设施运行监测与智能告警平台

面向新能源充电设施运维场景的后端管理平台，实现设备台账管理、运行数据上报、实时状态监测、异常告警和统计分析。

## MVP 核心闭环

```text
登录认证 → 设备管理 → 数据上报 → Redis 实时状态 → 基础阈值告警 → 告警查询/确认/恢复 → 运行概览
```

**当前版本：MVP v1（第一阶段完成）**

---

## 技术栈

| 技术 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.x | 后端框架 |
| JDK | 17 | 运行环境 |
| MyBatis-Plus | 3.x | ORM / 分页 / 逻辑删除 |
| MySQL | 8.0+ | 关系数据库 |
| Redis | 6/7 | 实时状态缓存 / 在线集合 |
| Sa-Token | 1.x | 登录认证 / 权限控制 |
| Knife4j | 4.x | 接口文档 |
| Hutool | 5.x | 工具类（SHA-256 等） |
| Lombok | — | 简化代码 |

---

## 项目结构

```text
charge-monitor-platform/
├── README.md                           # 项目总说明
├── docs/                               # 项目文档
│   ├── 01-需求说明.md                   # 项目背景、目标、功能范围
│   ├── 02-系统架构设计.md               # 架构设计、分层说明
│   ├── 03-数据库设计.md                 # 表结构、字段、索引、枚举
│   ├── 04-接口文档说明.md               # 接口路径、参数、响应、状态码
│   ├── 05-告警规则设计.md               # 告警规则体系设计
│   └── 06-部署运行文档.md               # 部署运行说明
├── sql/                                # 数据库脚本
│   ├── init.sql                        # 建库 + 建表 + 初始化数据（一步到位）
│   └── demo_data.sql                   # 额外演示数据（可选）
└── charge-monitor-backend/             # 后端模块
    ├── pom.xml                         # Maven 依赖
    └── src/main/java/com/ccbcc/charge/monitor/
        ├── ChargeMonitorApplication.java   # 启动类
        ├── common/                         # 公共模块
        │   ├── constants/                  # 常量（RedisKeyConstants 等）
        │   ├── exception/                  # 全局异常处理
        │   └── result/                     # 统一返回结构
        ├── config/                         # 配置类（Sa-Token / Redis / MyBatis-Plus / Knife4j）
        ├── module/                         # 业务模块
        │   ├── auth/                       # 认证模块（登录 / 退出 / 用户信息）
        │   ├── user/                       # 用户模块（Entity / Mapper）
        │   ├── device/                     # 设备模块（设备管理 / 数据上报 / 状态查询）
        │   ├── alarm/                      # 告警模块（告警查询 / 确认 / 恢复）
        │   └── report/                     # 报表模块（今日运行概览）
        ├── task/                           # 定时任务
        │   ├── DeviceOfflineCheckTask.java # 设备离线检测（第二阶段完善）
        │   └── DailyReportTask.java        # 运行日报（第二阶段完善）
        └── simulator/                      # 模拟器
            └── DeviceDataSimulator.java    # 设备数据模拟器（第二阶段完善）
```

### 分层说明

每个业务模块统一采用以下分层：

```text
controller/   → 接收请求、参数校验、调用 Service
dto/          → 接收前端请求参数
vo/           → 返回给前端的视图对象
entity/       → 数据库实体（与表结构一一对应）
mapper/       → MyBatis-Plus Mapper 接口
service/      → 业务逻辑接口
service/impl/ → 业务逻辑实现
```

---

## 数据库表

### MVP 核心表（6 张）

| 表名 | 说明 |
|---|---|
| `sys_user` | 用户表 |
| `sys_role` | 角色表（admin / operator / viewer） |
| `sys_user_role` | 用户角色关联表 |
| `device_info` | 设备信息表（台账） |
| `device_data` | 设备运行数据表（历史遥测） |
| `alarm_record` | 告警记录表 |

### 第二阶段预留表（5 张）

| 表名 | 说明 |
|---|---|
| `alarm_rule` | 告警规则表（表结构已完成，数据已预置） |
| `work_order` | 工单表（表结构已完成） |
| `work_order_log` | 工单流转日志表（表结构已完成） |
| `operation_log` | 操作日志表（表结构已完成） |
| `daily_report` | 运行日报表（表结构已完成） |

---

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6/7

### 2. 初始化数据库

```bash
# 连接 MySQL 执行 init.sql
mysql -u root -p < sql/init.sql
```

该脚本会：
- 创建 `charge_monitor` 数据库
- 创建 11 张表（6 张 MVP + 5 张第二阶段预留）
- 初始化 3 个角色、3 个用户、5 台设备、4 条告警规则、4 条运行数据、1 条示例告警

### 3. 配置环境变量

```bash
# MySQL 用户名（必填）
export MYSQL_USERNAME=root

# MySQL 密码（必填）
export MYSQL_PASSWORD=your_password

# Redis 密码（如果 Redis 有密码则必填，无密码可留空）
export REDIS_PASSWORD=
```

### 4. 启动项目

```bash
cd charge-monitor-backend

# 编译
mvn clean package -DskipTests

# 启动
mvn spring-boot:run
```

或者直接在 IDE（IntelliJ IDEA）中运行 `ChargeMonitorApplication.java`。

### 5. 验证启动

启动成功后访问：

- 接口文档（Knife4j）：http://localhost:8080/doc.html
- Swagger UI：http://localhost:8080/swagger-ui.html
- API 基础路径：http://localhost:8080/api

---

## 接口概览

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 认证 | POST | `/api/auth/login` | 用户登录 |
| 认证 | GET | `/api/auth/userInfo` | 获取当前用户信息 |
| 认证 | POST | `/api/auth/logout` | 用户退出 |
| 设备 | POST | `/api/device` | 新增设备 |
| 设备 | PUT | `/api/device/{id}` | 修改设备 |
| 设备 | DELETE | `/api/device/{id}` | 删除设备（逻辑删除） |
| 设备 | GET | `/api/device/page` | 分页查询设备 |
| 设备 | GET | `/api/device/{id}` | 查询设备详情 |
| 数据 | POST | `/api/device/data/report` | 上报设备运行数据 |
| 数据 | GET | `/api/device/data/latest/{deviceCode}` | 查询设备最新状态 |
| 数据 | GET | `/api/device/data/history` | 查询设备历史数据 |
| 告警 | GET | `/api/alarm/record/page` | 分页查询告警 |
| 告警 | GET | `/api/alarm/record/{id}` | 查询告警详情 |
| 告警 | PUT | `/api/alarm/record/{id}/ack` | 确认告警 |
| 告警 | PUT | `/api/alarm/record/{id}/recover` | 恢复告警 |
| 报表 | GET | `/api/report/overview` | 今日运行概览 |

详细接口文档见 [docs/04-接口文档说明.md](docs/04-接口文档说明.md)。

---

## 测试账号

| 用户名 | 密码 | 角色 | 说明 |
|---|---|---|---|
| admin | 123456 | admin（系统管理员） | 拥有全部权限 |
| operator | 123456 | operator（运维人员） | 设备状态查看、告警确认和恢复 |
| viewer | 123456 | viewer（只读用户） | 只能查看 |

---

## 测试 JSON

常用接口测试 JSON 见 [docs/test-json/](docs/test-json/) 目录：

| 文件 | 对应接口 |
|---|---|
| `login.json` | 登录 |
| `create-device.json` | 新增设备 |
| `update-device.json` | 修改设备 |
| `data-report-normal.json` | 上报正常数据 |
| `data-report-temp-alarm.json` | 上报触发温度告警 |
| `data-report-volt-alarm.json` | 上报触发电压告警 |
| `data-report-delay-alarm.json` | 上报触发网络延迟告警 |

---

## Redis Key 约定

| Key | 类型 | 说明 |
|---|---|---|
| `device:status:{deviceCode}` | String (JSON) | 设备最新运行状态 |
| `device:heartbeat:{deviceCode}` | String | 设备最近心跳时间 |
| `device:online:set` | Set | 当前在线设备编号集合 |
| `device:alarm:set` | Set | 当前存在未恢复告警的设备编号集合 |

---

## MVP 告警规则

当前规则写死在代码中（第二阶段改为 `alarm_rule` 表动态配置）：

| 指标 | 条件 | 告警等级 | 类型 |
|---|---|---|---|
| temperature（温度） | `> 80℃` | 3（严重） | THRESHOLD |
| voltage（电压） | `< 180V` | 2（重要） | THRESHOLD |
| networkDelay（网络延迟） | `> 200ms` | 1（一般） | THRESHOLD |

---

## 告警去重规则

- 去重维度：`deviceCode + alarmType + alarmMetric + 未恢复状态`
- 同一设备同一指标未恢复时，不新增记录，只更新 `alarm_count` 和 `last_time`
- 告警恢复后再次触发则生成新告警

---

## 第二阶段规划

按优先级排列：

1. **设备离线检测定时任务** — 定时扫描心跳超时设备，自动标记离线并生成 OFFLINE 告警
2. **抽取 AlarmDetectService** — 将告警检测逻辑从 DeviceDataServiceImpl 中独立出来
3. **动态告警规则表** — 启用 `alarm_rule` 表，实现后台可配置告警规则
4. **RabbitMQ 异步告警** — 数据上报与告警检测解耦
5. **WebSocket 实时推送** — 告警产生、状态变化、设备离线等实时推送
6. **工单流转模块** — 告警自动建单、派发、处理、关闭，形成运维闭环

详见 [docs/01-需求说明.md](docs/01-需求说明.md) 第 11 节。

---

## 项目文档索引

| 文档 | 内容 |
|---|---|
| [01-需求说明.md](docs/01-需求说明.md) | 项目背景、目标用户、业务流程、功能范围、验收标准 |
| [02-系统架构设计.md](docs/02-系统架构设计.md) | 系统架构、技术选型、分层设计 |
| [03-数据库设计.md](docs/03-数据库设计.md) | 表结构、字段说明、索引、枚举值 |
| [04-接口文档说明.md](docs/04-接口文档说明.md) | 全部接口路径、参数、响应、状态码 |
| [05-告警规则设计.md](docs/05-告警规则设计.md) | 告警规则体系、去重策略、扩展方向 |
| [06-部署运行文档.md](docs/06-部署运行文档.md) | 环境要求、部署步骤、配置说明 |

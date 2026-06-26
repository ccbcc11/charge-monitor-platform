# 新能源充电设施运行监测与智能告警平台

本项目面向新能源充电设施运维场景，MVP 第一版聚焦后端核心链路：

```text
登录认证 -> 设备管理 -> 数据上报 -> Redis 实时状态 -> 基础阈值告警 -> 告警查询 -> 简单报表
```

后端模块位于：

```text
charge-monitor-backend
```

技术栈：

```text
Spring Boot 3
JDK 17
MyBatis-Plus
MySQL
Redis
Sa-Token
Knife4j
```

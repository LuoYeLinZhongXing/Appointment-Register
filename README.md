# 医疗预约挂号系统 (Appointment Register System)

## 项目简介

这是一个基于Spring Boot 3.1.5开发的医疗预约挂号系统后端项目，采用前后端分离架构设计。系统支持患者在线挂号、医生排班管理、订单处理、排队叫号等核心功能，适用于医院或诊所的数字化管理需求。

## 技术栈

### 后端技术
- **Spring Boot 3.1.5** - 核心框架
- **Java 17** - 编程语言
- **MyBatis-Plus 3.5.3.1** - ORM框架
- **MySQL 8.0** - 关系型数据库
- **Redis 7** - 缓存和分布式锁
- **Redisson** - Redis客户端
- **JWT** - Token认证机制
- **Knife4j** - API文档工具
- **Lombok** - 简化代码开发
- **Maven** - 项目构建工具

### 架构特点
- 多模块Maven项目结构
- RESTful API设计风格
- 基于JWT的无状态认证
- Redis缓存优化性能
- 完整的日志记录和监控
- 统一异常处理机制

## 功能模块

### 核心功能
1. **用户管理**
   - 患者注册/登录/信息管理
   - 医生注册/登录/信息管理
   - 管理员账户管理

2. **科室管理**
   - 科室信息维护
   - 科室状态管理
   - 科室负责人设置

3. **号源管理**
   - 医生排班设置
   - 号源发布与管理
   - 时间段配置（上午/下午/晚上）

4. **预约挂号**
   - 在线挂号预约
   - 订单生成与管理
   - 支付流程集成
   - 取号报到功能

5. **排队叫号**
   - 实时排队队列
   - 医生叫号系统
   - 过号处理机制
   - 优先级排队

6. **系统管理**
   - 操作日志记录
   - 数据统计分析
   - 权限控制体系

## 项目结构
appointment-register/ 
├── ar-common/ # 公共模块 
│ ├── constant/ # 常量定义 
│ ├── context/ # 上下文工具 
│ ├── exception/ # 自定义异常 
│ ├── util/ # 工具类 │ └── Result.java # 统一返回结果 
├── ar-pojo/ # 数据传输对象 
│ ├── dto/ # 数据传输对象 
│ ├── entity/ # 实体类 
│ └── vo/ # 视图对象
├── ar-server/ # 服务模块
│ ├── annotation/ # 自定义注解 
│ ├── aspect/ # 切面编程 
│ ├── config/ # 配置类
│ ├── controller/ # 控制器层
│ ├── handler/ # 异常处理器 
│ ├── interceptor/ # 拦截器
│ ├── mapper/ # 数据访问层 
│ ├── service/ # 业务逻辑层 
│ ├── task/ # 定时任务 
│ └── ArApplication.java # 启动类 
├── sql/ # 数据库脚本 
├── Dockerfile # Docker配置
├── docker-compose.yml # Docker编排 
└── pom.xml # Maven配置

## 数据库设计

系统包含以下核心数据表：
- `admin` - 管理员表
- `patient` - 患者表  
- `doctor` - 医生表
- `dept` - 科室表
- `slot` - 号源表
- `order` - 订单表
- `queue` - 排队表
- [log] - 日志表

### 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.6+

### 本地部署

1. **克隆项目**
bash git clone <repository-url>
cd appointment-register
2. **数据库初始化**
sql
-- 创建数据库
   CREATE DATABASE appointment_register CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
3. **配置环境变量**
在 `application-dev.yml` 中配置：
yaml
ap:
 datasource:
  host: localhost
  port: 3306
  database: appointment_register
  username: your_username
  password: your_password
 redis:
  host:localhost
  port: 6379
  password: your_redis_password
  jwt: key: your_jwt_secret_key
4. **启动项目**
   bash
   mvn clean install
   cd ar-server
   mvn spring-boot:run
5. **访问API文档**
   http://localhost:8080/swagger-ui.html
## API接口说明

### 认证相关
- `POST /patient/login` - 患者登录
- `POST /doctor/login` - 医生登录
- `POST /admin/login` - 管理员登录

### 患者功能
- `POST /patient/register` - 患者注册
- `GET /patient/queue` - 查看排队信息
- `POST /order/register` - 挂号预约
- `GET /order/patient/orders` - 查看我的订单

### 医生功能
- `GET /doctor/current-queue` - 查看当前队列
- `POST /doctor/call-next` - 叫号
- `POST /doctor/start-treatment/{id}` - 开始治疗
- `POST /doctor/complete-treatment/{id}` - 完成治疗

### 管理功能
- `POST /slot/release` - 发布号源
- `POST /dept/create` - 创建科室
- `GET /order/page` - 订单管理

## 性能优化

### 缓存策略
- Redis缓存热门数据
- 号源信息预加载
- 队列信息实时同步

### 监控机制
- 方法执行时间监控
- 缓存命中率统计
- 系统健康检查

## 安全特性

- JWT Token认证
- 请求签名验证
- SQL注入防护
- XSS攻击防范
- 敏感信息加密存储

## 开发规范

### 代码风格
- 遵循阿里巴巴Java开发手册
- 使用Lombok简化代码
- 统一异常处理机制
- 完善的日志记录

### API设计
- RESTful风格接口
- 统一响应格式
- 详细的接口文档
- 参数校验完善
## 部署建议

### 生产环境配置
1. 配置HTTPS证书
2. 设置合适的JVM参数
3. 配置负载均衡
4. 启用数据库主从复制
5. 设置Redis集群

### 监控告警
- 应用性能监控(APM)
- 数据库性能监控
- 系统资源监控
- 错误日志告警

## 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交更改
4. 发起Pull Request

## 许可证

本项目采用Apache 2.0许可证

## 联系方式

- 作者：落叶林中行
- 邮箱：anlinwei_2022@qq.com

## 致谢

感谢所有开源项目的贡献者，特别感谢：
- Spring Boot团队
- MyBatis-Plus团队
- Redisson团队
- Knife4j团队



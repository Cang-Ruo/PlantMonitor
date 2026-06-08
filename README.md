# PlantMonitor - 植物生长智能监测系统

## 项目简介

PlantMonitor 是一款基于 Android 平台的植物生长状态智能监测与远程控制系统 APP，集成了**定时拍照**、**AI 植物识别**、**环境监测**、**远程控制**等功能，适用于农业种植、家庭园艺等场景。

## 核心功能

### 1. 角色选择系统
- ✅ **双角色架构**：首次启动时选择角色（监控端或控制端）
- ✅ **监控端**：负责定时拍照、传感器数据采集、AI识别、云端上传
- ✅ **控制端**：负责远程控制设备、查看监测数据、同步历史记录

### 2. 定时拍照监测
- ✅ **灵活定时设置**：支持秒级（1-300秒）和分钟级（5/15/30/60分钟）定时拍照
- ✅ **后台保活**：使用前台服务 + WorkManager 确保定时任务持续运行
- ✅ **自动识别**：拍照后自动进行 AI 植物识别
- ✅ **远程拍照**：控制端可远程触发监控端拍照

### 3. AI 植物识别
- ✅ **百度 AI 集成**：调用百度植物识别 API，支持花烛、白掌、绿萝等多种植物
- ✅ **通义千问分析**：使用 Qwen AI 提供详细的植物养护建议
- ✅ **健康诊断**：综合水温、气温、湿度等多维度评估植物健康状态
- ✅ **历史记录**：保存识别记录，支持离线查看

### 4. 环境监测
- ✅ **水温监测**：实时监测水培溶液温度
- ✅ **气温监测**：监测环境空气温度
- ✅ **湿度监测**：监测空气湿度
- ✅ **数据联动**：拍照时自动记录当时的环境数据

### 5. 远程控制
- ✅ **MQTT 协议**：通过 MQTT 与 OneNET 物联网平台通信
- ✅ **设备控制**：补光灯（led）、水泵（pump）、风扇（fan）
- ✅ **状态反馈**：实时显示 MQTT 连接状态
- ✅ **命令记录**：保存所有控制命令历史

### 6. 云端存储与同步
- ✅ **阿里云 OSS**：拍摄照片自动上传到阿里云对象存储
- ✅ **数据同步**：监控端与控制端数据实时同步
- ✅ **历史同步**：控制端可同步监控端的历史记录

### 7. AI 聊天助手
- ✅ **智能问答**：与 AI 助手"小植"进行对话交流
- ✅ **上下文理解**：支持多轮对话，记住聊天历史
- ✅ **纠错功能**：可纠正 AI 识别错误，重新分析

## 技术架构

### 架构模式
- **MVVM**：Model-View-ViewModel 架构，解耦 UI 与业务逻辑
- **Repository 模式**：统一管理数据源（本地数据库 + 网络 API）
- **单例模式**：MqttManager、Repository 等核心组件采用单例管理

### 核心技术栈

| 模块 | 技术选型 | 说明 |
|------|---------|------|
| 开发语言 | Java | 兼容性好，便于调试 |
| 最低 SDK | Android 10 (API 29) | 覆盖主流设备 |
| 目标 SDK | Android 15 (API 35) | 最新 Android 版本 |
| 架构模式 | MVVM + LiveData + ViewModel | 解耦 UI 与数据 |
| 相机 | CameraX | 生命周期感知，简化开发 |
| 网络通信 | Retrofit + OkHttp | 封装 HTTP 请求 |
| MQTT | Eclipse Paho | MQTT 客户端库 |
| 图像加载 | Glide | 高效加载本地/网络图片 |
| 本地数据库 | Room | 存储分析结果、控制记录 |
| 云存储 | 阿里云 OSS | 照片云端存储 |
| 物联网平台 | OneNET AIoT | MQTT 通信与设备管理 |
| 后台任务 | WorkManager + Timer | 实现定时拍照 |
| 前台服务 | Service | 保活服务，确保后台运行 |
| 图表展示 | MPAndroidChart | 数据可视化 |
| 导航 | Jetpack Navigation | 页面导航管理 |

## 项目结构

```
app/src/main/java/com/example/mygraduationproject/
├── ui/                    # UI 界面层
│   ├── monitor/          # 监测页面（相机预览、定时拍照）
│   ├── analysis/         # 分析页面（AI 识别结果展示）
│   ├── control/          # 控制页面（设备开关控制）
│   ├── settings/         # 设置页面（参数配置）
│   ├── chat/             # 聊天页面（AI 对话）
│   └── role/             # 角色选择页面
├── data/                  # 数据层
│   ├── AppDatabase.java  # Room 数据库
│   ├── *Dao.java         # 数据访问对象
│   └── Repository.java   # 数据仓库
├── network/               # 网络层
│   ├── ApiService.java   # AI 服务 API
│   ├── DeviceControlApi.java  # 设备控制接口
│   ├── BaiduAIApi.java   # 百度 AI 接口
│   ├── RetrofitClient.java    # Retrofit 客户端
│   ├── request/          # 请求实体类
│   └── response/         # 响应实体类
├── model/                 # 数据模型
│   ├── AIResult.java     # AI 识别结果
│   ├── PlantImage.java   # 植物图片
│   ├── ControlCommand.java  # 控制命令
│   ├── GrowthRecord.java    # 生长记录
│   ├── PlantRecord.java     # 植物记录
│   └── ChatMessage.java  # 聊天消息
├── mqtt/                   # MQTT 通信模块
│   └── MqttManager.java  # MQTT 管理器
├── service/               # 服务层
│   └── CameraService.java  # 相机前台服务
├── worker/                # 后台任务
│   └── AutoCaptureWorker.java  # 定时拍照 Worker
├── camera/                # 相机模块
│   └── CameraManager.java  # 相机管理器
├── receiver/              # 广播接收器
│   └── BootReceiver.java  # 开机自启接收器
├── utils/                 # 工具类
│   ├── PreferenceManager.java  # 偏好设置管理
│   ├── ImageUtils.java   # 图像压缩工具
│   ├── Base64Utils.java  # Base64 编码工具
│   ├── DateUtils.java    # 日期工具
│   ├── OssUploadUtils.java  # 阿里云 OSS 上传
│   ├── HealthEvaluator.java  # 健康评估工具
│   └── HistoryRecordsManager.java  # 历史记录管理
├── config/                # 配置类
│   └── ApiConfig.java    # API 配置（API Key 等）
└── PlantMonitorApp.java   # Application 类
```

## 快速开始

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android 10 (API 29) 及以上设备

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <项目地址>
   cd AndroidProject
   ```

2. **配置 API Key**

   打开 `app/src/main/java/com/example/mygraduationproject/config/ApiConfig.java`，修改以下配置：

   ```java
   // 百度 AI 配置（植物识别）
   public static final String BAIDU_API_KEY = "你的百度 API_KEY";
   public static final String BAIDU_SECRET_KEY = "你的百度 SECRET_KEY";

   // 通义千问配置（详细分析）
   public static final String QWEN_API_KEY = "你的通义千问 API_KEY";
   ```

3. **配置 OneNET 物联网平台**
   - 注册 OneNET 账号并创建产品
   - 配置产品 ID、API Key、设备名称

4. **配置阿里云 OSS**
   - 注册阿里云账号并开通 OSS 服务
   - 配置 AccessKey ID 和 AccessKey Secret

5. **编译运行**
   ```bash
   # 使用 Android Studio 打开项目
   # 点击 Run 按钮或使用快捷键 Shift+F10
   ```

## OneNET 物联网平台接口说明

### MQTT 连接配置
- **服务器地址**: `tcp://183.230.40.96:1883`
- **产品 ID**: 你的 OneNET 产品 ID
- **认证方式**: Token 认证

### 控制命令 API (HTTP)

**接口地址**: `POST https://iot-api.heclouds.com/thingmodel/set-device-property`

**请求体**:
```json
{
  "product_id": "你的产品ID",
  "device_name": "TT",
  "params": {
    "led": true,
    "pump": false,
    "fan": true
  }
}
```

**设备代码**:
- `led` - 补光灯
- `pump` - 水泵
- `fan` - 风扇

**动作**:
- `true` - 开启
- `false` - 关闭

### 传感器属性
- `water_temp` - 水温
- `air_temp` - 气温
- `air_hum` - 空气湿度

## 功能演示

### 角色选择

1. 首次打开 APP，进入角色选择页面
2. 选择 **监控端 (WiFi)** 或 **控制端 (4G)**
3. 角色选择后保存在本地，下次启动自动进入主界面

### 定时拍照设置（监控端）

1. 打开 APP，进入"监测"页面
2. 开启"定时拍照"开关
3. 进入"设置"页面，配置拍照间隔：
   - **快速选择**：5/15/30/60 分钟
   - **自定义间隔**：输入数值，选择单位（秒/分钟），点击"设置"
4. 返回"监测"页面，查看当前间隔显示

### 远程拍照（控制端）

1. 进入"控制"页面
2. 点击"立即拍照"按钮
3. 监控端收到指令后执行拍照并上传
4. 查看远程状态提示

### AI 识别流程

1. 点击"拍照"按钮或等待定时拍照
2. APP 自动压缩图片并上传到阿里云 OSS
3. 调用百度 AI 进行植物识别
4. 调用通义千问提供详细分析（植物简介、养护要点、常见问题、健康建议）
5. 综合水温、气温、湿度计算健康评分
6. 结果保存到历史记录

### 远程控制流程（控制端）

1. 进入"控制"页面
2. 点击设备开关（如补光灯）
3. APP 通过 MQTT 发送控制指令到 OneNET
4. OneNET 转发指令到 M5 硬件设备
5. 显示控制结果（成功/失败）

## 数据库设计

### AIResult 表（AI 识别结果）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| imageId | Long | 关联图片 ID |
| imagePath | String | 图片路径 |
| plantName | String | 植物名称 |
| plantType | String | 植物类型（花烛/白掌/绿萝/其他） |
| confidence | Double | 置信度 |
| healthScore | Float | 健康评分 |
| healthStatus | String | 健康状态 |
| waterTemp | Float | 水温 |
| airTemp | Float | 气温 |
| airHumidity | Float | 空气湿度 |
| detailedAnalysis | String | 详细分析 |
| timestamp | Long | 时间戳 |

### ControlCommand 表（控制命令）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| device | String | 设备代码 |
| action | String | 动作 |
| sendTime | Long | 发送时间 |
| status | String | 状态（成功/失败） |

### GrowthRecord 表（生长记录）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| imageUrl | String | 图片 URL |
| waterTemp | Float | 水温 |
| airTemp | Float | 气温 |
| airHumidity | Float | 空气湿度 |
| healthScore | Float | 健康评分 |
| healthStatus | String | 健康状态 |
| plantType | String | 植物类型 |
| timestamp | Long | 时间戳 |

## 常见问题

### 1. 定时拍照不触发？
- 检查是否开启"定时拍照"开关
- 确保 APP 有前台服务权限
- 查看通知栏是否有"植物监测服务"通知

### 2. MQTT 连接失败？
- 检查网络连接
- 验证 OneNET 产品 ID 和 API Key 配置
- 查看 Logcat 日志排查错误

### 3. AI 识别失败？
- 检查网络连接
- 验证 API Key 配置是否正确
- 查看 Logcat 日志排查错误

### 4. OSS 上传失败？
- 检查阿里云 AccessKey 配置
- 确认 Bucket 名称正确
- 检查网络连接

## 项目亮点

1. **双角色架构**：监控端与控制端分离，各司其职
2. **完整物联网方案**：MQTT + OneNET 实现设备间通信
3. **灵活定时拍照**：支持秒级和分钟级自定义间隔
4. **双 AI 引擎**：百度 AI（识别）+ 通义千问（分析），专业且详细
5. **多维度健康评估**：综合水温、气温、湿度、置信度评估
6. **云端同步**：阿里云 OSS 存储 + OneNET 数据同步
7. **完善的后台保活**：前台服务 + WorkManager + 开机自启
8. **优雅的 UI 设计**：Material Design 风格，绿色主题契合植物主题

## 后续优化方向

1. 支持更多植物种类的识别和养护建议
2. 增加数据图表展示，可视化植物生长趋势
3. 支持多设备管理，可扩展到多个监测点
4. 添加告警功能，异常情况及时通知用户
5. 增加植物百科和养护日历功能

## 许可证

MIT License

## 联系方式

如有问题或建议，欢迎提交 Issue 或 Pull Request。
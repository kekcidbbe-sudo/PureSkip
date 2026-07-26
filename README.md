# 纯净跳过 (PureSkip)

## 项目描述
Android 广告弹窗屏蔽应用。使用无障碍服务自动跳过已适配应用的启动广告，离线优先、确定性规则、默认不动作。

## 产品信息
- **产品名称**: 纯净跳过 / PureSkip
- **包名**: `com.nzsk.pureskip`
- **当前版本**: 1.9.6 (`versionCode 19`) ✅
- **最低支持**: Android 9 (API 28)
- **目标版本**: Android 16 (API 36)
- **编译版本**: Android 16 (API 36)

## 技术栈
- **语言**: Kotlin
- **UI框架**: Android Views + Material Design 3
- **构建工具**: Gradle 8.x + Kotlin DSL
- **最低SDK**: API 28 (Android 9)
- **目标SDK**: API 36 (Android 16)
- **编译SDK**: API 36 (Android 16)

## 核心架构
```
AccessibilityEvent → AppPolicy → SafetyGuard → RuleMatcher → ActionExecutor → 本地最小记录
```

## 模块结构
| 模块 | 职责 |
|------|------|
| `accessibility` | 无障碍服务、窗口监听、教学准星与确认按钮 |
| `engine` | 候选控件识别、规则匹配、动作执行 |
| `rules` | 内置离线适配规则（10个首批应用） |
| `safety` | 敏感场景排除、频率限制、防误触 |
| `settings` | 用户开关、应用控制、本地偏好 |
| `privacy` | 权限说明、数据查看、清除功能 |
| `ui` | 首次引导、主页、应用控制、隐私中心 |

## 首批适配应用
| 分类 | 应用 |
|------|------|
| 视频 | 腾讯视频、爱奇艺、优酷、芒果TV、搜狐视频 |
| 小说 | 番茄小说、七猫免费小说、起点读书、QQ阅读、掌阅 |

## MVP功能范围
- [x] 首次启动引导说明
- [x] 无障碍服务状态显示
- [x] 启动广告自动跳过（10秒时间窗口）
- [x] 按应用控制开关
- [x] 按应用增强识别（默认关闭，严格限定用户选中的应用）
- [x] 已适配、最近使用、全部已安装三类应用列表
- [x] 手动教学、已学规则停用与删除
- [x] 启动阶段/应用全程作用范围
- [x] 安全控制（防抖、次数限制、暂停、紧急停止）
- [x] 支付、密码、验证码、安装、授权等敏感页面拦截
- [x] 可选本地诊断（最近 100 条，不记录页面文字和截图）
- [x] 本地累计次数显示
- [x] 隐私中心

## 隐私与权限
- 不声明 `INTERNET` 权限，无云端、无埋点、无截图、无 OCR。
- 无障碍权限用于读取可识别控件并执行点击，应用内必须向用户明确说明。
- `QUERY_ALL_PACKAGES` 只用于用户确认后显示“全部已安装应用”选择器，不上传、不统计。

## 构建说明
```bash
# 调试版
./gradlew assembleDebug

# JVM 单元测试
./gradlew testDebugUnitTest

# 连接模拟器或真机后的设备测试
./gradlew connectedDebugAndroidTest

# 发布版（需要签名配置）
./gradlew assembleRelease
```

完整的增强识别使用与测试说明见 [`doc/1.8.0-按应用增强识别与测试说明.md`](doc/1.8.0-按应用增强识别与测试说明.md)；1.8.1 真机回归修复见 [`doc/1.8.1-真机回归修复说明.md`](doc/1.8.1-真机回归修复说明.md)；1.8.2 精准识别和顺滑教学优化见 [`doc/1.8.2-精准识别与顺滑教学优化说明.md`](doc/1.8.2-精准识别与顺滑教学优化说明.md)；1.8.3 后台扫描降温修复见 [`doc/1.8.3-后台扫描降温修复说明.md`](doc/1.8.3-后台扫描降温修复说明.md)；1.9.0~1.9.3 识别增强与发热修复见 [`doc/1.9.0-1.9.3-Android识别增强与发热修复说明.md`](doc/1.9.0-1.9.3-Android识别增强与发热修复说明.md)；1.9.6 评论区误触与循环点击修复见 [`doc/1.9.6-抖音评论区误触与循环点击修复说明.md`](doc/1.9.6-抖音评论区误触与循环点击修复说明.md)。

## 目录结构
```
doc/                # 文档目录
prototype/          # 产品原型目录
project/frontend/   # Android 应用源代码
project/backend/    # 本项目无后端（纯本地应用）
database/           # 数据库脚本目录（本项目使用 SharedPreferences）
utils/              # 项目工具包目录
```

## 签名密钥
- 密钥文件: `keystore/pureskip.jks`（**缺失**，当前 `keystore/` 目录为空）
- 别名: `pureskip`
- **注意**: 签名密钥丢失后新版无法覆盖旧版安装，务必做好离线备份；构建 release 前需重新生成 JKS 并离线多份备份

## 启动目录
- Android 工程根：`project/frontend`
- 构建命令：`./gradlew :app:assembleDebug`
- 调试 APK 输出：`project/frontend/app/build/outputs/apk/debug/app-debug.apk`

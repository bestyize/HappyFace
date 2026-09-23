# HappyFace

HappyFace 是一套为桌面 AI 智能终端设计的参数化表情系统。表情在**整块矩形屏幕**内生成；猫耳、身体与其他配件属于屏幕外的可装配硬件。首要目标是让同一角色在网页、电脑、手机及未来 Android 智能盒子上保持一致的表情语言。

> 当前状态：P0 工程起点。仓库含共享 Kotlin 表情核心、Compose 绘制示例及桌面／网页预览入口；尚未完成 Android、iOS 应用、云端对话或发布包。不要将预览视为公开测试版。

## 技术路线

- **Kotlin Multiplatform** 管理表情意图、参数、自然过渡和本地微动作。
- **Compose Multiplatform Canvas** 绘制屏幕脸；没有整张表情图片轮播，也不把猫耳画入屏幕。
- **本地互动优先**：触碰立即响应；云端 DeepSeek 未来只负责语言理解和高层表情意图，不能逐帧驱动渲染。
- **语音可选**：Tencent Cloud STT/TTS 留在后续里程碑，首发不依赖麦克风。

## 目录

| 路径 | 内容 |
| --- | --- |
| `shared` | 可重用表情状态和 Compose 画布；目前配置 desktop JVM + Kotlin/Wasm |
| `desktopApp` | Windows/macOS 预览窗口入口；悬浮小窗能力待 P3 实现 |
| `webApp` | Kotlin/Wasm 预览入口；浏览器兼容性待 P0 实测 |
| `docs/IMPLEMENTATION_PLAN.md` | 阶段计划与验收标准 |
| `docs/EXPRESSION_CONTRACT.md` | 表情参数与事件协议草案 |

## 本地运行

需要 JDK 17+、支持 WasmGC 的现代浏览器。仓库包含 Gradle Wrapper，首次运行会下载 Gradle。当前执行环境无法完成依赖下载，因此以下命令**尚待在开发机验证**。

```bash
./gradlew :desktopApp:run
./gradlew :webApp:wasmJsBrowserDevelopmentRun
./gradlew :shared:desktopTest
```

## GitHub 自动打包

向 `main` 推送代码会自动运行 [Package previews](https://github.com/bestyize/HappyFace/actions/workflows/package.yml)，也可以在仓库的 **Actions → Package previews → Run workflow** 手动触发。成功后在该次运行页面底部的 **Artifacts** 下载：

| 产物 | 用途 |
| --- | --- |
| `HappyFace-web` | 网页静态文件；解压后部署文件夹内容到静态站点 |
| `HappyFace-windows-x64` | Windows x64 `.msi` 安装包 |
| `HappyFace-macos-arm64` | Apple Silicon Mac `.dmg` 安装包 |

本地体验网页包：下载并解压 `HappyFace-web`，进入包含 `index.html` 的目录，启动静态文件服务：

```bash
python3 -m http.server 8000 --bind 127.0.0.1
```

然后在浏览器打开 <http://localhost:8000/>。不要直接用 `file://` 打开 `index.html`；网页包中的 Wasm 资源需要通过 HTTP 加载。请使用支持 WasmGC 的现代浏览器。

产物保存 14 天。当前为未签名的 P0 预览包，不会自动公开部署或发布 Release；Android / iOS 应用模块尚未建立，因此暂不生成 APK / IPA。网页任务同时运行表情引擎测试。可在对应系统上本地执行 `:desktopApp:packageMsi` 或 `:desktopApp:packageDmg`，以及 `:webApp:wasmJsBrowserDistribution`。首次构建结果以 Actions 运行记录为准。

网页构建依赖 Compose Multiplatform Web Beta 的兼容性。P0 会在 iPhone Safari、Android 浏览器和目标级别的 Android 设备上做技术闸门测试。如浏览器端不达标，可复用 `ExpressionEngine` 并单独替换网页绘制层。

## 当前交互

启动后可见完整屏幕表情，含低幅呼吸和不固定间隔的眨眼；点击屏幕时，眼睛平滑看向触碰位置并进入好奇表情。它是一个供设计与性能实验使用的最小原型。

## 产品阶段

1. P0：参数规范、基础跨端预览及 Android 实机验证。
2. P1：基础表情、设计师调参面板、过渡与本地触碰体验。
3. P2：可分享的网页测试版，接入受控文字 AI 对话。
4. P3：Windows/macOS 桌面悬浮伙伴。
5. P4：Android/iOS 全屏应用及三款盒子屏幕适配。
6. P5：系统小组件；随后按硬件方案接入语音、图像识别和可装配身体。

详细范围与验收标准见 [实施计划](docs/IMPLEMENTATION_PLAN.md)。

# GentleUI 架构与移植说明

## 分层

```
┌──────────────────────────── core（纯 Java，无 Minecraft 依赖）────────────────────────────┐
│ math / anim / color      向量与仿射、噪声、缓动；解析阻尼弹簧 Spring、Tween、全局 Motion    │
│ render                   Canvas：矢量图形细分为抗锯齿四边形；Icon 矢量图标；Paint 渐变     │
│ theme (+liquid/material/glass)  Theme 接口、Palette、Skin（控件绘制）、Backdrop、标题场景  │
│ ui                       Button / Slider / Choice / TextField / ScrollPanel 等自绘控件     │
│ scene                    UiScreen、标题画面基类、设置、布局编辑器、暂停菜单、加载视图       │
│ config                   GentleConfig + Gson 编解码、版本号与 sanitize、原子写入           │
│ host                     Host 接口、Actions 动作协议、Keys 键位空间（GLFW 兼容值）          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
                 ▲ 只通过 Canvas / Host / Keys 三个契约与游戏交互
┌──────────────────────────── fabric/<version>（每个 Minecraft 版本一份）──────────────────┐
│ render/McCanvas, MeshElement, ImageElement, Images   Canvas 后端                        │
│ McHost, ActionRunner, WorldCache                     Host 实现与动作执行                 │
│ screen/GentleScreen, InputMap                        UiScreen ↔ Screen 桥接与输入转换    │
│ ScreenRouter + 路由 mixin                            标题/暂停画面替换                   │
│ skin/*  + 渲染 mixin                                 原版与模组菜单的统一皮肤            │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

**core 不引用任何 Minecraft 类**，只在编译期依赖 Gson（运行时由游戏提供）。所有主题、控件、动画与配置逻辑都在 core 中，并有不依赖游戏的单元测试（`RecordingCanvas` 记录绘制调用用于断言）。适配器负责把 core 的三个契约落到具体版本的 API 上，因此新版本移植只改适配器。共享资源（语言文件、图标）放在 `core/src/main/resources`，由各适配器的 jar 一并打包。

## 渲染方案

`Canvas` 把圆角矩形、椭圆、弧线、折线、阴影、光晕和渐变全部细分为四边形，每个形状外缘附加约 1 物理像素宽的透明渐变边带实现抗锯齿（与 NanoVG 的做法相同），因此不需要自定义着色器或多重采样。阴影是解析计算的同心圆角矩形环，光晕环数随形状的像素半径变化。

适配器的 `emitQuad` 把顶点累积到 `MeshElement`，作为一个 `GuiElementRenderState` 提交到原版 GUI 渲染状态，使用原版 `RenderPipelines.GUI`（位置 + 颜色，QUADS）。这样 GentleUI 的图形与原版控件处于同一延迟渲染队列，层级、裁剪（scissor）与 GUI 缩放都与原版一致。原版 GUI 管线开启背面剔除，所以所有四边形必须保持原版的顶点绕序。文字走原版字体渲染；`crispScale` 会把文字缩放量取整到整数物理像素，避免位图字体发虚。图片由 `ImageElement` 以纹理四边形提交。

## 皮肤拦截点

统一菜单风格不针对具体界面类，而是在少数绘制咽喉点上拦截，所以对原版与使用原版控件的模组界面同样有效。`SkinContext` 在每个界面渲染开始时激活、结束时关闭，只有激活期间的调用会被替换；游戏内 HUD 不经过这些路径。

| 拦截点（1.21.11 名称） | 作用 |
| --- | --- |
| `GuiGraphics.blitSprite(RenderPipeline, Identifier, int×5)` | 按钮、滑块、输入框、标签页、滚动条等精灵图，交给 `SpriteSkin` 以主题重绘 |
| `GuiGraphics.blit(RenderPipeline, Identifier, …13 参数)` | 菜单背景、标签页底板、分隔线等纹理，交给 `TextureSkin` |
| `GuiRenderState.submitText` | 文字颜色改写与去阴影（`TextRemap`，亮色主题） |
| `Screen.renderWithTooltipAndSubtitles` | 界面级开始/结束标记，内容前后插入主题背景与前景层 |
| `Screen.renderBackground` | 用主题背景替换原版的泥土/模糊背景 |
| `AbstractSelectionList.renderListBackground / renderListSeparators / renderSelection / renderItem` | 列表容器收窄为实际行宽的面板、选中与悬停高亮 |
| `LevelLoadingScreen.renderBackground / render` | 世界加载进度改由 `LoadingView` 绘制 |
| `Minecraft.setScreen`（26.x 为 `Gui.setScreen`） | `ScreenRouter` 替换标题与暂停画面；`setScreen(null)` 在无世界时隐式创建的标题画面也在此显式创建以便路由 |
| `Screen.renderPanorama / renderBlurredBackground` | 自行重写了背景方法的状态界面（读取存档、加载资源、保存世界）从全景图开始画背景，在这里换成主题背景；无世界时不做模糊 |
| `Minecraft.setScreenAndShow` | 这类界面只会被强制渲染一帧，随后游戏阻塞读盘，所以跳过入场动画，否则停在透明的第一帧 |
| `FocusableTextWidget.renderWidget` | 状态界面里黑底描边的文字框换成主题卡片 |
| `TabButton.renderFocusUnderline`（26.x 为 `MenuTabBar.MenuTabButton`） | 主题标签页自带选中标记，去掉原版叠在上面的白色下划线 |

`skinExclusions` 中列出的界面类不会激活 `SkinContext`。

GentleUI 不调用 Fabric API 的代码，但要靠它在正式环境里加载模组自带的语言文件（原版资源管理器只读取游戏本体），所以 `fabric.mod.json` 把它列为依赖。画布需要访问 `GuiGraphics` 的 `guiRenderState` 和 `scissorStack`，这两项写在自己的访问加宽器里，不借用 Fabric API 的。Fabric API 只放在开发客户端的运行时类路径上，不参与编译，避免它的访问加宽器掩盖缺失的条目。

开发客户端会把 `core` 的资源直接放在类路径上，所以语言文件在开发环境里总能读到，正式环境的问题在这里看不出来。发布前应该用 Loom 的 `ClientProductionRunTask` 拿构建出来的 jar 跑一次正式环境。

## 主题系统

`Theme` 提供调色板、控件皮肤 `Skin`、背景 `Backdrop`、标题场景工厂与界面音效。`Themes.register(id, factory)` 注册主题，内置三套在客户端入口注册；第三方模组可以用同样的方式加入主题，设置界面会自动列出。

所有动效都由 `Spring`（解析解的阻尼弹簧，帧率无关）驱动，`Motion` 统一施加全局动画速度与"减弱动态效果"。控件动画状态以屏幕矩形为键缓存，原版控件每帧重建也能保持连续的悬停、按下过渡。

标题场景中的每个可布局元素都实现 `TitleElement`，布局编辑器通过统一的用户变换（偏移、缩放、不透明度、隐藏）调整它们，用户新增的元素保存在 `layouts.<theme>.custom` 中，与主题内置元素一起参与吸附与命中测试。

## 移植到新的 Minecraft 版本

1. 复制最近的适配器目录，例如 `cp -r fabric/26.3 fabric/<新版本>`，在 `settings.gradle` 中加入新项目，修改其 `gradle.properties` 中的 `minecraft_version`、`loader_version`、`fabric_api_version`。
2. 选择 Loom 插件：带混淆的版本（≤ 1.21.x）用 `net.fabricmc.fabric-loom-remap` 加官方映射，访问加宽器命名空间为 `named`；不混淆的版本（26.x 起）用 `net.fabricmc.fabric-loom`，命名空间为 `official`。按游戏要求设置 `options.release`。
3. 编译适配器，按编译错误逐个处理重命名。mixin 目标是字符串，不会产生编译错误，必须在开发客户端中启动一次确认没有注入失败（mixin 配置中 `defaultRequire: 1`，任何目标失配都会在启动时报错）。
4. 检查输入码：core 使用 GLFW 兼容的键码与鼠标按钮编号，适配器的 `InputMap` 负责转换。
5. 在开发客户端中检查标题画面（三套主题）、Liquid 拖拽与多级环、设置与布局编辑器、选项/视频设置/按键绑定/语言/资源包/多人游戏/创建世界等原版界面、暂停菜单与世界加载画面。

### 1.21.11 → 26.3 的实际变化

| 方面 | 1.21.11 | 26.3 |
| --- | --- | --- |
| 混淆与映射 | 混淆，Loom remap + 官方映射 | 不混淆，Loom 无 remap；访问加宽器用 `official` |
| Java | 21 | 25 |
| 绘制上下文 | `GuiGraphics` | `GuiGraphicsExtractor` |
| 界面绘制方法 | `render`、`renderBackground`、`renderBlurredBackground`、`renderWithTooltipAndSubtitles` | `extractRenderState`、`extractBackground`、`extractBlurredBackground`、`extractRenderStateWithTooltipAndSubtitles` |
| 列表方法 | `renderListBackground` / `renderListSeparators` / `renderSelection` / `renderItem` | `extract…` 同名替换 |
| 渲染状态包 | `net.minecraft.client.gui.render.state` | `net.minecraft.client.renderer.state.gui` |
| 提交元素与文字 | `submitGuiElement`、`submitText`、`drawString` | `addGuiElement`、`addText`、`text` |
| GPU 抽象 | `com.mojang.blaze3d.pipeline.RenderPipeline` 等 | `com.mojang.renderpearl.api.*`（mixin 描述符字符串需同步修改） |
| 文字渲染状态 | 公有字段 | 私有字段，统一通过 `GuiTextRenderStateAccessor` 读取 |
| 界面管理 | `Minecraft.setScreen` / `Minecraft.screen` | `Gui.setScreen` / `Gui.screen()`，路由 mixin 改为 `GuiMixin` |
| 字幕 | `gui.renderDeferredSubtitles()` | `gui.hud.extractDeferredSubtitles()` |
| 窗口与输入 | GLFW 键码、修饰键与鼠标按钮 | SDL 扫描码（如 `A`=4、`Esc`=41）、修饰键掩码与按钮编号（左 1、右 3），由 `InputMap` 转换 |
| 翻译查询 | `I18n.exists` | `Language.getInstance().has` |
| 链接与目录 | `confirmLinkNow(Screen, String)`、`Util.getPlatform().openPath` | `confirmLinkNow(Screen, URI)`、`Blaze3D.openPath` |
| 对局域网开放 | `ShareToLanScreen` | 并入 `WorldOptionsScreen` |

core 在这次移植中没有任何改动。

## 构建与测试

```bash
export JAVA_HOME=/path/to/jdk-25     # Loom 1.19 需要 Gradle 9.8 + JDK 25
./gradlew build                      # 两个适配器 + core 单元测试
./gradlew :core:test                 # 仅核心测试
./gradlew :fabric-1.21.11:runClient
./gradlew :fabric-26.3:runClient
```

开发客户端的配置与世界位于各适配器的 `run/` 目录（已被 git 忽略），GentleUI 配置在 `run/config/gentleui/gentleui.json`。

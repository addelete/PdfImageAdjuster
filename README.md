# PDF 图片调色器

一个功能强大的 PDF 图片颜色调整工具，基于 Kotlin Multiplatform 和 Apache PDFBox 开发。

## 项目简介

PDF 图片调色器是一款专业的 PDF 文档图片处理工具，可以对 PDF 文件中的图片进行精细的颜色调整。支持 HSL（色相、饱和度、亮度）调整和曲线调整，提供预设管理功能，让您轻松实现批量图片处理。

## 主要功能

- **PDF 文档浏览**
  - 支持加载和浏览 PDF 文档
  - 页面缩略图导航
  - 文档大纲（书签）支持
  - 快速页面跳转

- **图片颜色调整**
  - HSL 调整：色相（-180° ~ 180°）、饱和度（-100 ~ 100）、亮度（-100 ~ 100）
  - 曲线调整：支持 RGB、R、G、B 四个通道的独立曲线编辑
  - 实时预览：调整参数后即时查看效果
  - 选择性应用：可选择应用到当前图片、当前页面或整个文档

- **预设管理**
  - 保存常用的调整参数为预设
  - 快速加载和应用预设
  - 预设导入/导出功能
  - 预设重命名和删除

- **批量处理**
  - 支持对整个 PDF 文档的所有图片批量应用调整
  - 处理进度实时显示
  - 可随时取消处理

- **自动更新**
  - 启动时自动检查新版本
  - 一键下载最新版本

## 系统要求

- **操作系统**：Windows 10/11、macOS 10.14+、Linux
- **Java 运行环境**：JDK 17 或更高版本
- **内存**：建议 4GB 以上
- **磁盘空间**：至少 200MB 可用空间

## 安装说明

### 下载预编译版本

1. 前往 [Releases](https://github.com/yourusername/PdfImageAdjuster/releases) 页面
2. 下载适合您操作系统的安装包：
   - Windows: `PdfImageAdjuster-x.x.x.exe`
3. 运行安装程序并按照提示完成安装

### 从源码构建

#### 前置要求

- JDK 17 或更高版本
- Gradle（项目自带 Gradle Wrapper）

#### 构建步骤

1. 克隆仓库：
```bash
git clone https://github.com/yourusername/PdfImageAdjuster.git
cd PdfImageAdjuster
```

2. 运行应用（开发模式）：
```bash
./gradlew run
```

3. 构建安装包：
```bash
# 构建所有平台的安装包
./gradlew packageDistributionForCurrentOS

# 或者构建特定格式
./gradlew packageMsi    # Windows MSI
./gradlew packageExe    # Windows EXE
./gradlew packageDmg    # macOS DMG
./gradlew packageDeb    # Linux DEB
```

构建完成后，安装包将位于 `composeApp/build/compose/binaries/main/` 目录下。

## 使用说明

### 基本操作

1. **打开 PDF 文件**
   - 点击顶部工具栏的"打开 PDF"按钮
   - 选择要处理的 PDF 文件

2. **浏览 PDF**
   - 使用左侧的页面缩略图快速跳转到指定页面
   - 使用大纲（书签）导航文档结构
   - 在顶部输入页码直接跳转

3. **选择图片**
   - 在左侧图片列表中点击要调整的图片
   - 选中的图片会在右侧预览区域显示

### 颜色调整

#### HSL 调整

1. 点击图片列表中的"调整"按钮打开调整面板
2. 使用滑块调整参数：
   - **色相（Hue）**：-180° ~ 180°，改变图片的整体色调
   - **饱和度（Saturation）**：-100 ~ 100，调整颜色的鲜艳程度
   - **亮度（Lightness）**：-100 ~ 100，调整图片的明暗程度
3. 实时预览调整效果

#### 曲线调整

1. 切换到"曲线"标签页
2. 选择要调整的通道：RGB、R、G、B
3. 在曲线编辑器中点击添加控制点
4. 拖动控制点调整曲线形状
5. 右键点击控制点可以删除

### 应用调整

1. 完成参数调整后，点击"应用"按钮
2. 选择应用范围：
   - **当前图片**：仅应用到选中的图片
   - **当前页面**：应用到当前页面的所有图片
   - **整个文档**：应用到 PDF 文档的所有图片
3. 确认后开始处理，可以查看处理进度
4. 处理完成后可以撤销操作

### 预设管理

1. **保存预设**
   - 调整好参数后，点击"保存为预设"按钮
   - 输入预设名称并保存

2. **加载预设**
   - 在预设列表中点击预设名称
   - 参数会自动加载到调整面板

3. **管理预设**
   - 重命名：点击预设旁的编辑按钮
   - 删除：点击预设旁的删除按钮
   - 导出：将预设保存为 JSON 文件
   - 导入：从 JSON 文件加载预设

### 保存 PDF

1. 完成所有调整后，点击顶部工具栏的"保存 PDF"按钮
2. 选择保存位置和文件名
3. 等待保存完成

## 技术栈

- **Kotlin Multiplatform** - 跨平台开发框架
- **Compose Desktop** - 现代化的声明式 UI 框架
- **Apache PDFBox** - PDF 文档处理库
- **Kotlinx Serialization** - 数据序列化
- **Kotlinx Coroutines** - 异步编程
- **Ktor Client** - HTTP 客户端（用于版本检查）

## 项目结构

```
PdfImageAdjuster/
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── data/              # 数据层
│       │   ├── domain/            # 业务逻辑层
│       │   │   ├── models/        # 数据模型
│       │   │   └── processors/    # 图片处理器
│       │   ├── platform/          # 平台抽象
│       │   └── ui/                # UI 层
│       │       ├── components/    # UI 组件
│       │       └── screens/       # 页面
│       └── jvmMain/kotlin/        # JVM 平台实现
├── gradle/                        # Gradle 配置
├── LICENSE                        # Apache 2.0 协议
└── README.md                      # 项目说明
```

## 贡献指南

欢迎贡献代码、报告问题或提出建议！

### 如何贡献

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

### 报告问题

如果您发现了 bug 或有功能建议，请在 [Issues](https://github.com/yourusername/PdfImageAdjuster/issues) 页面提交。

## 许可证

本项目采用 Apache License 2.0 许可证 - 详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

本项目使用了以下优秀的开源项目：

- [Kotlin Multiplatform](https://kotlinlang.org/compose-multiplatform/) - 跨平台开发框架
- [Apache PDFBox](https://pdfbox.apache.org/) - PDF 处理库


# 🎯 面试准备助手 - Interview Preparation Assistant

> 一个现代化的交互式面试准备工具，专为 AI、大数据、Java 后端和全栈开发岗位设计

[![GitHub Pages](https://img.shields.io/badge/Demo-Live-success)](你的GitHub Pages链接)
[![HTML5](https://img.shields.io/badge/HTML5-E34F26?logo=html5&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/HTML)
[![CSS3](https://img.shields.io/badge/CSS3-1572B6?logo=css3&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/CSS)
[![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?logo=javascript&logoColor=black)](https://developer.mozilla.org/en-US/docs/Web/JavaScript)

## ✨ 功能特性

### 📱 响应式设计
- **完美适配**：支持桌面端、平板和移动端
- **统一体验**：手机端底部抽屉卡片，电脑端居中弹窗卡片
- **毛玻璃效果**：现代化 UI 设计，视觉效果出色

### 🔍 智能搜索
- **实时搜索**：输入关键词即时高亮显示所有匹配结果
- **结果分组**：按标签页自动分组，快速定位内容
- **上下文预览**：显示搜索结果的上下文，帮助快速判断相关性
- **键盘导航**：支持 Enter / Shift+Enter 快速切换搜索结果
- **侧边栏面板**：固定侧边栏，搜索不受页面滚动影响

### 📚 知识点速查
- **术语标注**：所有技术术语都带有读音和详细解释
- **交互式卡片**：
  - 手机端：点击显示底部抽屉卡片，支持滑动关闭
  - 电脑端：悬浮显示居中卡片，自动关闭
- **分区显示**：读音和知识点独立显示，清晰易读

### 🎨 可视化演示
集成多个交互式可视化组件，帮助理解复杂概念：

#### Java 后端
- **红黑树动画**：分步演示左旋、右旋、变色操作，支持播放/暂停/速度调节
- **JVM 内存结构**：可视化堆、栈、方法区等内存区域
- **GC 算法演示**：标记-清除、复制、标记-整理算法对比
- **线程池工作流程**：核心线程、队列、非核心线程、拒绝策略可视化

#### 大数据技术
- **Flink 架构**：JobManager、TaskManager、Checkpoint 流程演示
- **Kafka 架构**：Producer、Broker、Consumer、分区机制可视化
- **Hudi 表类型**：COW vs MOR 对比演示

#### AI 技术
- **RAG 流程**：检索增强生成完整流程可视化
- **向量检索**：Embedding、Milvus 检索原理演示

#### 容器技术
- **K8s 架构**：Master、Worker、核心组件关系图

### 🗂️ 内容分类

#### 1️⃣ AI 大模型技术
- vLLM、PyTorch、CUDA、FlashAttention2
- RAG、Text2SQL、Embedding
- CogVLM 多模态大模型
- 昇腾 910B 适配经验

#### 2️⃣ AI 技术栈详解
- Kafka、Flink、Hudi 详细原理
- Redis、MySQL、K8s、Docker
- 性能优化和故障排查

#### 3️⃣ 大数据盘点技术
- 国产化适配（飞腾 CPU、达梦数据库、统信 UOS）
- DataNucleus、Hive、HDFS
- ARM 架构适配

#### 4️⃣ Java 后端技术
- Spring Boot、HashMap、ArrayList vs LinkedList
- Java 并发（synchronized、Lock、线程池）
- JVM 内存模型、GC 算法、性能调优

#### 5️⃣ 前端基础知识
- Vue.js、React 框架对比
- TypeScript、Webpack/Vite
- 性能优化、HTTP/浏览器原理

#### 6️⃣ Python 基础
- 数据类型、迭代器、生成器
- 装饰器（深度讲解）
- GIL 全局解释器锁（深度讲解）
- 生成器与 yield（深度讲解）

#### 7️⃣ 项目介绍话术
6 个完整项目，包含时间线、技术栈、项目亮点

#### 8️⃣ 应对策略
- 国产化适配经验
- 昇腾 910B 显卡适配
- 技术选型原因
- 项目难点和问题解决
- 跨部门协作经验
- 大规模数据处理策略

## 🚀 快速开始

### 在线访问
直接访问 GitHub Pages: `https://你的用户名.github.io/仓库名/面试准备-完整版.html`

### 本地运行
```bash
# 克隆仓库
git clone https://github.com/你的用户名/仓库名.git

# 进入目录
cd 仓库名

# 使用浏览器打开
# 方法1: 直接双击 面试准备-完整版.html
# 方法2: 使用本地服务器
python -m http.server 8000
# 然后访问 http://localhost:8000/面试准备-完整版.html
```

## 📖 使用指南

### 桌面端操作
1. **查看术语解释**：鼠标悬浮在彩色术语上，自动弹出卡片
2. **搜索内容**：
   - 在顶部搜索框输入关键词
   - 点击右上角 📋 图标打开搜索结果面板
   - 使用 ↑/↓ 按钮或 Enter/Shift+Enter 导航
3. **查看可视化**：点击各个技术点中的演示按钮
4. **切换标签**：点击顶部导航切换不同技术栈

### 移动端操作
1. **查看术语解释**：点击彩色术语，底部弹出抽屉卡片
2. **关闭卡片**：
   - 点击卡片外的遮罩层
   - 点击右上角 ✕ 关闭按钮
   - 向下滑动卡片
3. **搜索内容**：同桌面端
4. **折叠内容**：点击各个技术栈标题展开/收起

## 🛠️ 技术栈

- **前端框架**：纯 HTML5 + CSS3 + JavaScript（无依赖）
- **动画效果**：CSS3 Animation + Transition
- **响应式设计**：Media Query + Flexbox
- **可视化**：SVG + JavaScript 动画
- **UI 设计**：毛玻璃效果、渐变色、阴影

## 📂 项目结构

```
.
├── 面试准备-完整版.html    # 主文件（单页应用）
└── README.md               # 项目说明文档
```

## 🎯 适用场景

- 🎓 **应届生/在校生**：系统学习面试必考知识点
- 💼 **社招工程师**：快速复习技术栈，准备面试
- 📱 **移动学习**：随时随地在手机上复习
- 🔍 **快速查询**：面试前快速搜索特定技术点

## 🌟 特色亮点

1. **零依赖**：单个 HTML 文件，无需安装任何依赖
2. **离线可用**：下载后可完全离线使用
3. **视觉现代**：采用最新的 UI 设计趋势
4. **交互流畅**：精心调优的动画和过渡效果
5. **内容丰富**：覆盖 AI、大数据、Java、前端、Python 全栈
6. **持续更新**：根据最新面试趋势更新内容

## 📝 内容统计

- 📌 **技术术语**：300+ 个
- 📖 **知识点**：200+ 个
- 🎨 **可视化演示**：10+ 个
- 💼 **项目案例**：6 个
- 📊 **对比表格**：5+ 个

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 👤 作者

**任相鹏**
- 📧 Email: renxp@example.com
- 💼 专注于：AI、大数据、Java 后端、全栈开发

## 🙏 致谢

感谢所有为本项目提供建议和反馈的朋友们！

---

⭐ 如果这个项目对你有帮助，欢迎给个 Star！

📮 有任何问题或建议，欢迎提 Issue 讨论！

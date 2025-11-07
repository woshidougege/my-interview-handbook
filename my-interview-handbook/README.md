# 我的面试手册 (My Interview Handbook)

一个基于 Next.js 的个人面试准备平台，支持多人配置、Markdown 内容管理、全文搜索、术语提示等功能。

## ✨ 特性

- 📚 **多人支持**：按人员分类管理面试资料
- 📝 **Markdown 编辑**：frontmatter + 术语标记语法
- 🔍 **全文搜索**：模糊搜索，快速定位内容
- 💡 **术语提示**：自定义术语发音和解释
- 🎨 **美观UI**：响应式设计，支持桌面和移动端
- 🛠️ **后台管理**：在线编辑内容（开发环境）
- 💾 **自动备份**：每次保存自动创建带时间戳的备份
- 🚀 **静态导出**：可部署到 GitHub Pages 等静态服务器

## 🏗️ 技术栈

- **框架**：Next.js 14+ (App Router)
- **语言**：TypeScript
- **样式**：Tailwind CSS
- **Markdown**：gray-matter + react-markdown
- **搜索**：fuse.js
- **部署**：静态导出

## 📦 安装和运行

### 1. 安装依赖

```bash
npm install
```

### 2. 开发模式

```bash
npm run dev
```

访问：
- 前台：`http://localhost:3000`
- 后台：`http://localhost:3000/admin`

### 3. 生产构建

```bash
npm run build
```

### 4. 静态导出

```bash
npm run build
# 静态文件生成在 out/ 目录
```

## 📁 项目结构

```
my-interview-handbook/
├── src/
│   ├── app/                    # Next.js 页面
│   │   ├── page.tsx           # 主页面
│   │   ├── admin/page.tsx     # 后台管理
│   │   └── api/               # API Routes
│   ├── components/            # React 组件
│   │   ├── PersonSelector.tsx
│   │   ├── TabNavigation.tsx
│   │   ├── SearchBox.tsx
│   │   └── MarkdownRenderer.tsx
│   ├── lib/                   # 工具函数
│   └── types/                 # TypeScript 类型
├── content/                   # 内容文件（Markdown）
│   ├── 任相鹏/
│   │   ├── profile.json      # 人员配置
│   │   ├── 01-resume.md      # 简历
│   │   └── ...
│   └── 张晓雪/
│       └── ...
├── backups/                   # 自动备份（不提交到 Git）
└── public/                    # 静态资源
```

## 📝 内容管理

### 人员配置 (`profile.json`)

```json
{
  "name": "任相鹏",
  "title": "AI开发工程师",
  "avatar": "/avatars/avatar.jpg",
  "tabs": [
    {
      "id": "resume",
      "title": "个人简历",
      "icon": "📋",
      "file": "01-resume.md",
      "order": 1
    }
  ]
}
```

### Markdown 文件格式

```markdown
---
title: "个人简历"
icon: "📋"
order: 1
description: "个人基本信息"
---

## 内容

这是正文内容，支持 [[Flink|夫林克|Apache Flink 流式计算引擎]] 术语标记。
```

### 术语语法

```markdown
[[术语|发音|解释说明]]
```

示例：
```markdown
我熟练掌握 [[Flink|夫林克|Apache Flink，实时流式计算引擎]] 技术。
```

## 🚀 部署到 GitHub Pages

### 方法一：手动部署

1. 构建项目：
```bash
npm run build
```

2. 提交到 GitHub：
```bash
git add .
git commit -m "Build project"
git push origin main
```

3. 在 GitHub 仓库设置：
   - Settings → Pages
   - Source: Deploy from a branch
   - Branch: `main` / `out` folder

### 方法二：GitHub Actions 自动部署

创建 `.github/workflows/deploy.yml`：

```yaml
name: Deploy to GitHub Pages

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Setup Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '20'
    
    - name: Install dependencies
      run: npm ci
    
    - name: Build
      run: npm run build
    
    - name: Deploy
      uses: peaceiris/actions-gh-pages@v3
      with:
        github_token: ${{ secrets.GITHUB_TOKEN }}
        publish_dir: ./out
```

## 🛠️ 开发指南

### 添加新人员

1. 在 `content/` 下创建人员目录（如 `张三/`）
2. 创建 `profile.json` 配置文件
3. 添加 Markdown 内容文件

### 编辑内容

**方式一：后台管理**（推荐）
1. 运行 `npm run dev`
2. 访问 `http://localhost:3000/admin`
3. 选择文件编辑
4. 点击保存（自动备份）

**方式二：直接编辑**
- 直接修改 `content/` 目录下的 Markdown 文件

### 备份恢复

备份文件位于 `backups/人员名/文件名.YYYY-MM-DD_HH-mm-ss.md`

恢复步骤：
1. 找到需要恢复的备份文件
2. 复制内容到对应的 `content/` 文件
3. 或重命名备份文件替换原文件

## 🎨 自定义

### 修改主题色

编辑 `tailwind.config.ts` 和组件中的颜色类名。

### 添加新功能

1. 在 `src/components/` 添加新组件
2. 在 `src/app/page.tsx` 中引入和使用

## 📄 许可

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**Happy interviewing! 🎉**

# 快速开始

## 🚀 第一次运行

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

### 3. 访问应用

- **前台页面**：`http://localhost:3000`
- **后台管理**：`http://localhost:3000/admin`

## 📝 编辑内容

### 方式一：使用后台管理（推荐）

1. 访问 `http://localhost:3000/admin`
2. 选择人员和文件
3. 在编辑器中修改内容
4. 点击"保存"按钮
5. 系统会自动创建备份

### 方式二：直接编辑文件

1. 打开 `content/人员名/文件名.md`
2. 使用任何文本编辑器编辑
3. 保存文件
4. 刷新浏览器

## 🎯 术语标记语法

在 Markdown 中使用以下语法标记术语：

```markdown
[[术语|发音|解释说明]]
```

**示例：**

```markdown
我熟练掌握 [[Flink|夫林克|Apache Flink，实时流式计算引擎，支持流批一体处理]] 技术。
```

**渲染效果：**

- 术语会显示为紫色加粗下划线
- 鼠标悬停或点击显示发音和解释
- 移动端点击弹出卡片

## 👥 添加新人员

### 1. 创建人员目录

```bash
mkdir content/新人员名
```

### 2. 创建配置文件

在 `content/新人员名/profile.json` 中：

```json
{
  "name": "新人员名",
  "title": "职位",
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

### 3. 创建内容文件

在 `content/新人员名/01-resume.md` 中：

```markdown
---
title: "个人简历"
icon: "📋"
order: 1
description: "个人基本信息"
---

## 基本信息

...你的内容...
```

### 4. 刷新浏览器

新人员会自动出现在人员选择器中。

## 💾 备份管理

### 查看备份

备份文件位于：`backups/人员名/文件名.YYYY-MM-DD_HH-mm-ss.md`

### 恢复备份

1. 找到需要的备份文件
2. 复制内容到对应的 `content/` 文件
3. 或直接重命名备份文件替换原文件

## 🔍 使用搜索

1. 在顶部搜索框输入关键词
2. 系统会模糊搜索所有人员的所有内容
3. 点击搜索结果自动跳转

## 🚀 部署到生产环境

### GitHub Pages 部署

1. 创建 GitHub 仓库并推送代码

```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/你的用户名/你的仓库名.git
git push -u origin main
```

2. 在 GitHub 仓库设置中启用 Pages

- Settings → Pages
- Source: GitHub Actions

3. 推送代码后自动部署

```bash
git push
```

### 自定义域名（可选）

1. 在仓库根目录创建 `public/CNAME` 文件
2. 写入你的域名（如 `handbook.example.com`）
3. 在域名服务商添加 CNAME 记录指向 `你的用户名.github.io`

## ❓ 常见问题

### Q: 后台管理页面显示"仅开发环境可用"

A: 后台管理仅在 `npm run dev` 时可用，生产环境（`npm run build`）不可用。

### Q: 如何修改主题颜色？

A: 编辑各组件中的 Tailwind 颜色类名，如 `bg-purple-500` 改为 `bg-blue-500`。

### Q: 术语提示不显示

A: 检查语法是否正确：`[[术语|发音|解释]]`，注意是双中括号。

### Q: 搜索功能不工作

A: 确保内容文件正确加载，刷新页面重新加载搜索索引。

## 🆘 需要帮助？

- 查看 `README.md` 获取详细文档
- 提交 Issue 到 GitHub 仓库
- 查看源代码注释

---

**开始你的面试准备之旅吧！🎉**


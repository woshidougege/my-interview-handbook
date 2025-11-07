---
title: "前端基础"
icon: "🎨"
order: 5
description: "Vue、React、Next.js 前端技术"
---

## Vue 3

### 核心特性

- **组合式 API**：setup() 函数
- **响应式系统**：Proxy 替代 Object.defineProperty
- **性能提升**：编译优化、Tree-shaking
- **TypeScript 支持**：更好的类型推导

### 常用 API

```javascript
import { ref, reactive, computed, watch } from 'vue'

// 响应式数据
const count = ref(0)
const state = reactive({ name: 'Vue' })

// 计算属性
const double = computed(() => count.value * 2)

// 监听器
watch(count, (newVal) => {
  console.log('count changed:', newVal)
})
```

---

## React

### [[React|瑞爱克特|组件化前端库]]

**核心概念**

- **虚拟 DOM**：diff 算法优化渲染
- **单向数据流**：props 传递数据
- **Hooks**：函数组件状态管理
- **JSX**：JavaScript + XML 语法

### 常用 Hooks

```javascript
import { useState, useEffect, useMemo } from 'react'

function Counter() {
  const [count, setCount] = useState(0)
  
  useEffect(() => {
    document.title = `Count: ${count}`
  }, [count])
  
  const double = useMemo(() => count * 2, [count])
  
  return <button onClick={() => setCount(count + 1)}>
    Count: {count}
  </button>
}
```

---

## Next.js

### [[Next.js|耐克斯特点J-S|React 服务端渲染框架]]

**核心特性**

- **SSR**：服务端渲染，SEO友好
- **SSG**：静态站点生成
- **API Routes**：内置API接口
- **文件路由**：基于文件系统的路由

---

## TypeScript

### [[TypeScript|泰普斯克瑞特|JavaScript 超集]]

**类型系统**

```typescript
// 接口
interface User {
  id: number
  name: string
  email?: string  // 可选属性
}

// 泛型
function identity<T>(arg: T): T {
  return arg
}

// 联合类型
type Status = 'pending' | 'success' | 'error'
```

---

## 前端工程化

### [[Vite|V特|新一代构建工具]]

- 极速冷启动（ESM）
- 热更新快
- 按需编译
- 生产构建基于 Rollup

### [[Webpack|外柏派克|模块打包工具]]

- 模块打包
- 代码分割
- 资源优化
- 插件生态丰富

---

## 面试常见问题

### Q1：Vue 和 React 的区别？

| 特性 | Vue | React |
|------|-----|-------|
| 模板 | Template | JSX |
| 状态管理 | Pinia | Redux/Zustand |
| 学习曲线 | 平缓 | 陡峭 |

### Q2：虚拟 DOM 是什么？

JavaScript 对象模拟真实 DOM，通过 diff 算法找出变化，批量更新真实 DOM，减少性能开销。


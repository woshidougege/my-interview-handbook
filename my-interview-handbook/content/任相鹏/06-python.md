---
title: "Python基础"
icon: "🐍"
order: 6
description: "Python 基础语法、常用库、面试题"
---

## Python 基础

### 数据类型

```python
# 基本类型
num = 42              # int
pi = 3.14             # float
name = "Python"       # str
is_ok = True          # bool

# 容器类型
lst = [1, 2, 3]                    # list (可变)
tpl = (1, 2, 3)                    # tuple (不可变)
st = {1, 2, 3}                     # set (去重)
dct = {"name": "Python"}           # dict (键值对)
```

### 列表推导式

```python
# 基本用法
squares = [x**2 for x in range(10)]

# 带条件
evens = [x for x in range(10) if x % 2 == 0]

# 嵌套
matrix = [[i+j for j in range(3)] for i in range(3)]
```

---

## 面向对象

### 类与继承

```python
class Animal:
    def __init__(self, name):
        self.name = name
    
    def speak(self):
        pass

class Dog(Animal):
    def speak(self):
        return f"{self.name} says Woof!"

dog = Dog("Buddy")
print(dog.speak())  # Buddy says Woof!
```

---

## 装饰器

### [[Decorator|德科瑞特|装饰器]]

```python
def timer(func):
    import time
    def wrapper(*args, **kwargs):
        start = time.time()
        result = func(*args, **kwargs)
        print(f"Time: {time.time() - start}s")
        return result
    return wrapper

@timer
def slow_function():
    time.sleep(1)
```

---

## GIL 全局解释器锁

### 什么是 [[GIL|G-I-L|Global Interpreter Lock]]？

CPython 的全局锁，同一时刻只允许一个线程执行 Python 字节码。

**影响**

- CPU 密集型：多线程无效
- IO 密集型：多线程有效（IO 时释放 GIL）

**解决方案**

- 多进程（multiprocessing）
- C 扩展（释放 GIL）
- 异步 IO（asyncio）

---

## 生成器

### [[Generator|珍呢瑞特|生成器]]

```python
def fibonacci(n):
    a, b = 0, 1
    for _ in range(n):
        yield a
        a, b = b, a + b

for num in fibonacci(10):
    print(num)
```

**优势**

- 惰性计算
- 节省内存
- 支持无限序列

---

## 常用库

### NumPy

```python
import numpy as np

arr = np.array([1, 2, 3, 4, 5])
print(arr.mean())  # 3.0
print(arr * 2)     # [2 4 6 8 10]
```

### Pandas

```python
import pandas as pd

df = pd.DataFrame({
    'name': ['Alice', 'Bob'],
    'age': [25, 30]
})
print(df[df['age'] > 25])
```

---

## 面试常见问题

### Q1：深拷贝 vs 浅拷贝？

- **浅拷贝**：复制对象引用
- **深拷贝**：递归复制所有对象

```python
import copy

original = [[1, 2], [3, 4]]
shallow = copy.copy(original)
deep = copy.deepcopy(original)
```

### Q2：*args 和 **kwargs？

- `*args`：接收任意数量的位置参数（tuple）
- `**kwargs`：接收任意数量的关键字参数（dict）

```python
def func(*args, **kwargs):
    print(args)    # (1, 2, 3)
    print(kwargs)  # {'a': 1, 'b': 2}

func(1, 2, 3, a=1, b=2)
```


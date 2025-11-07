---
title: "自动化测试"
icon: "🤖"
order: 3
description: "Selenium、Pytest 等自动化测试框架"
---

## Selenium

### 基本用法

```python
from selenium import webdriver

driver = webdriver.Chrome()
driver.get("https://example.com")
element = driver.find_element_by_id("username")
element.send_keys("testuser")
```

---

## Pytest

### 测试框架

```python
def test_addition():
    assert 1 + 1 == 2
```

---

（此文件可继续补充内容）


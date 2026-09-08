# LittleAnt DSL 参考（面向 AI 代码生成）

LittleAnt DSL 是一种会被 `AntDslConverter` 编译成模块图的 Python-like 小型语言，并不是 Python 解释器。模块名称、参数、返回值和默认值以 [LittleAnt AI API](ai-api-with-discription.md) 为准。

不能使用 Python 标准库、导入、类、对象属性、异常、文件、网络、推导式等完整 Python 功能。

## 1. 最小程序

程序由一个或多个入口组成，入口下方的缩进内容会编译成命令链：

```python
@ai_start
def init():
    say("ready")

@tick_start
def tick():
    if is_in_water():
        say("splash")
```

常用入口：

- `@ai_start`：AI 初始化时执行一次。
- `@tick_start`：每个游戏 tick 执行。
- `@receive_goal`：匹配的自定义目标开始时执行一次。
- `@goal_tick_start`：匹配的自定义目标活动期间每 tick 执行。

值得注意的是，`@receive_goal`和`@goal_tick_start`装饰器目前只能写裸模块名，不能传参。例如只能写 `@receive_goal`，不能写 `@receive_goal("mine")`。自定义目标名需要在模块图中设置。
同时`@receive_goal`和`@goal_tick_start`下一行的函数名为调用的goal名称。

被装饰器修饰过的函数严格来说不算函数，所以不能调用，比如：

```python
@receive_goal
def a():
    say("a")
    
@ai_start
def start():
    a()         # 错误
```


## 2. 行、缩进与注释

- 每行写一个语句。
- 空行以及忽略前导空格后以 `#` 开头的行会被忽略。
- 子句缩进必须严格大于父语句缩进；建议统一使用 4 个空格。
- 不要混用 tab 和空格，不要在行尾写分号。
- 名称区分大小写。
- 源代码最多 65536 个字符，编译后的模块图最多包含 256 个模块。
- 字符串可以使用单引号或双引号。

## 3. 模块调用与参数

普通语句是模块调用：

```python
say("hello")
move_to_xyz(10, 64, -2)
say(join_string_str("HP=", health()))
```

支持位置参数和命名参数：

```python
submit_background_goal(
    goal=better_float(),
    priority=2,
    move_flag=true(),
    look_flag=false(),
    jump_flag=true()
)
```

上面的多行写法仅用于展示参数；实际 DSL 应把一次模块调用写在同一行：

```python
submit_background_goal(goal=better_float(), priority=2, move_flag=true(), look_flag=false(), jump_flag=true())
```

参数规则：

- 位置参数按照 API 中的参数顺序绑定。
- 命名参数必须使用 API 中真实存在的参数名。
- 可以混合位置参数和命名参数。
- 未提供的参数使用模块默认值。
- 模块调用可以嵌套。
- 未注册的模块名会导致编译失败。

布尔常量必须写成 `true()` 和 `false()`，不能写 Python 的 `True` 和 `False`。

## 4. 普通变量

标量赋值会自动生成 `set_variable`：

```python
n = 3
message = "hello"
n = add(n, 1)
```

在普通 `TEXT`、`NUMBER` 或布尔表达式输入中，裸变量名会自动读取同名普通变量。因此：

```python
say(n)
n = n + 1
```

分别等价于：

```python
say(get_variable("n"))
n = add(get_variable("n"), 1)
```

普通变量在运行时以字符串保存。数字模块会尝试把字符串解析为数字；解析失败时通常返回 `0` 或使用模块默认值。

变量属于单只 ant 的黑板，不是整个游戏共享的全局变量。`set_variable_permanent("name")` 可以让当前值随 ant 持久保存。

## 5. 列表

### 5.1 列表格式

列表使用方括号表示：

```text
[]
[a,b,c]
[12,64,-5]
[[12,64,-5],[20,70,9]]
```

顶层逗号分隔列表元素；嵌套方括号内部的逗号不会分隔外层列表。位置是三元素列表 `[x,y,z]`，多个位置组成嵌套列表 `[[x1,y1,z1],[x2,y2,z2]]`。

### 5.2 列表赋值与自动类型识别

列表字面量会创建或替换同名列表：

```python
items = []
numbers = [1,2,3]
positions = [[1,64,2],[8,70,9]]
```

当右侧 reporter 在 `ModuleRegistry` 中声明为 `LIST` 输出时，赋值也会自动生成 `set_list_list`：

```python
target = find_nearest_block("minecraft:oak_log")
targets = find_block_list("minecraft:oak_log", 10)
position_copy = target
```

编译器会记住这些名称是列表变量。之后在参数或表达式中使用裸名称时会自动生成 `get_list`：

```python
if target != []:
    move_to_blockpos(target)
```

上例等价于：

```python
if get_list("target") != []:
    move_to_blockpos(get_list("target"))
```

不要在同一段 DSL 中让同一个名称一会儿保存普通变量、一会儿保存列表；变量类型由编译器按照源码顺序跟踪。

### 5.3 读取和修改列表

目前不支持 Python 下标语法 `items[0]`。使用列表模块访问元素，索引从 `0` 开始：

```python
items = ["minecraft:stone","minecraft:dirt"]
first = get_list_value("items", 0)
set_list_kv("items", 1, "minecraft:oak_log")
add_value("items", "minecraft:apple")
```

列表模块的 `name` 参数表示列表名称，因此必须传字符串名称，例如 `get_list_value("items", 0)`，不要写 `get_list_value(items, 0)`。

常用列表操作：

- `new_list("items")`：创建或清空列表。
- `get_list("items")`：返回完整列表。
- `get_list_value("items", index)`：读取一个元素，越界返回空字符串。
- `set_list_kv("items", index, value)`：设置元素；缺失的中间位置用空值补齐。
- `add_value("items", value)`：追加一个值。
- `add_list("items", other_list)`：追加另一个列表的所有顶层元素。
- `set_list_list("items", other_list)`：用另一个列表替换完整内容。
- `clear_list("items")`：删除当前列表内容。
- `set_list_permanent("items")`：持久保存当前列表快照。

读取嵌套位置列表中的第一个位置，可以直接把 reporter 嵌套到 `LIST` 参数：

```python
targets = find_block_list("minecraft:oak_log", 10)
if targets != []:
    move_to_blockpos(get_list_value("targets", 0))
```

也可以先保存成普通变量，但再次传给 `LIST` 输入时需要显式读取普通变量：

```python
first = get_list_value("targets", 0)
move_to_blockpos(get_variable("first"))
```

这是因为 `first` 是普通变量，而 `move_to_blockpos` 的输入类型是 `LIST`。

## 6. 算术表达式

支持 `+`、`-`、`*`、`/`、`%`：

```python
n = n + 1
remaining = total - used
area = width * height
half = total / 2
remainder = total % 64
result = (a + b) * 2
```

优先级与常规算术一致：括号最高，其次是 `* / %`，最后是 `+ -`；同级运算从左向右结合。

每个运算符都会编译为对应模块，也可以显式调用模块：

| 写法 | 等价模块 |
| --- | --- |
| `a + b` | `add(a, b)` |
| `a - b` | `subtract(a, b)` |
| `a * b` | `multiply(a, b)` |
| `a / b` | `divide(a, b)` |
| `a % b` | `mod(a, b)` |

另外可用 `absolute(number)` 和 `random(min,max)`。除数为零时，`divide` 和 `mod` 返回 `0`。

不要使用 `+=`、`-=`、`++` 或 `--`；这些复合赋值语法不受支持。

## 7. 条件与布尔表达式

支持以下运算：

- 比较：`==`、`!=`、`>`、`<`、`>=`、`<=`
- 逻辑：`and`、`or`、前缀 `not`
- `!condition` 也可以表示逻辑非，但推荐使用 `not`

```python
if health() >= 15 and not is_on_fire():
    say("safe")

if target == [] or is_hurt():
    say("need a new plan")
```

优先级依次为：括号、算术、比较、`not`、`and`、`or`。不要写 Python 链式比较 `0 < n < 10`，应改成：

```python
if n > 0 and n < 10:
    say("in range")
```

数值比较模块会先尝试按数字比较；无法解析为数字时会退回字符串比较。列表相等判断比较的是列表的运行时文本表示。

## 8. `if` / `elif` / `else`

支持标准缩进块：

```python
if health() > 15:
    say("healthy")
elif health() > 5:
    say("wounded")
else:
    say("critical")
```

转换器会把 `elif` 和 `else` 编译成嵌套的 `if_else` 模块。它们必须与对应的 `if` 保持相同缩进。

简单 `if` 支持单行形式：

```python
if is_hurt(): say("ouch")
```

单行 `if` 后不能继续连接 `elif` 或 `else`，单行子句也只能是模块调用，不能是赋值语句。

## 9. `repeat`、`for` 和 `while`

### 9.1 `repeat`

```python
repeat(3):
    jump()

repeat(n):
    say("again")
```

语法必须写成 `repeat(...)`，名称与左括号之间不要插入空格。

### 9.2 `for variable in range(...)`

`for` 会编译成普通的变量模块和 `repeat`，不会给 `repeat` 增加特殊参数。循环变量可以使用任意合法标识符，并能在循环体内直接访问：

```python
for i in range(3):
    say(i)
```

输出索引依次为 `0`、`1`、`2`。也支持起止范围：

```python
for slot in range(2, 5):
    say(slot)
    say(get_item_in_inventory(slot))
```

此时 `slot` 依次为 `2`、`3`、`4`。`start` 和 `stop` 都可以是数字变量或数值 reporter：

```python
for index in range(start, add(start, count)):
    say(index)
```

编译结果的逻辑等价于下面的模块链：

```python
# for i in range(start, stop):
i = start - 1
repeat(stop - start):
    i = i + 1
    # 原循环体
```

索引增量放在每轮开头，因此即使原循环体执行了 `continue()`，下一轮的索引仍会正常增加。这里的 `start - 1` 是编译器生成的内部初始值；循环体第一次运行时看到的仍然是 `start`。

当前仅支持 `range(stop)` 和 `range(start, stop)`：

- `range()` 会编译失败。
- `range(start, stop, step)` 会编译失败。
- 当 `stop <= start` 时执行 0 次。
- 不支持负步长。

循环变量是当前 ant 黑板中的普通变量；正常执行至少一次后，循环结束会保留最后一次迭代的值。如果范围为空，循环体不会执行，变量会保留编译器写入的内部初始值 `start - 1`。

### 9.3 `while`

```python
n = 3
while n > 0:
    say(n)
    n = n - 1
```

为了避免无限循环，单次 `while` 最多执行 1000 次。`while` 也支持单行模块调用：

```python
while is_in_water(): jump()
```

### 9.4 `break()` 与 `continue()`

它们是控制模块，因此需要带括号：

```python
for i in range(10):
    if i >= 5:
        break()
    if i % 2 == 0:
        continue()
    say(i)
```

只应在 `repeat`、`for` 或 `while` 的循环体内使用。

## 10. 自定义函数

普通 `def` 会生成一个 `function_start` 入口。调用已定义的函数名等价于 `call_function("name")`：

```python
def greet():
    say("hello")

@ai_start
def main():
    greet()
```

当前不支持函数参数、返回值或局部作用域：

```python
greet(1)       # 不支持
greet(x=1)     # 不支持
```

所有普通变量和列表仍属于当前 ant 的黑板。

## 11. Goal 调度

目标 reporter 只构造 `vanilla:...` 协议字符串；`submit_foreground_goal` 和 `submit_background_goal` 只提交目标并立即返回，不会等待目标完成。

在 `@tick_start` 中应检查重复目标，否则每个 tick 都可能再次提交：

```python
@tick_start
def mine_logs():
    target = find_nearest_block("minecraft:oak_log")
    if target != []:
        goal = break_block_blockpos(target)
        if not already_has_goal(goal):
            submit_foreground_goal(goal)
```

世界查询 reporter 每次求值都会重新搜索。先赋值再复用可以避免重复扫描，并保证后续操作使用同一个查询结果。

前景目标按 FIFO 顺序执行。后台目标按照优先级和 `move_flag`、`look_flag`、`jump_flag` 资源冲突进行调度。完整机制和各 goal reporter 的协议请查阅 [LittleAnt AI API](ai-api-with-discription.md)。

## 12. 完整示例

下面的脚本寻找最多 8 个原木位置，逐个显示坐标，并提交最近原木的破坏目标：

```python
@ai_start
def start():
    logs = find_block_list("minecraft:oak_log", 8)
    if logs == []:
        say("no logs")
    else:
        for i in range(0, 8):
            log_pos = get_list_value("logs", i)
            if log_pos != "":
                say(log_pos)

        nearest = get_list_value("logs", 0)
        submit_foreground_goal(break_block_blockpos(get_variable("nearest")))
```

注意：`find_block_list(..., 8)` 最多返回 8 个结果，不保证一定有 8 个；越界的 `get_list_value` 返回空字符串。

## 13. 明确不支持的 Python 语法

以下写法不要生成：

- `import`、`from ... import ...`
- 类、对象属性、方法调用和 `self`
- `try` / `except`、`with`、`raise`
- `return`、`yield`、`lambda`
- 列表下标 `items[0]`、切片和列表推导式
- `for value in some_list`；`for` 目前只能配合 `range`
- `range` 的步长参数
- `+=`、`-=` 等复合赋值
- Python 的 `True`、`False`、`None`
- 多目标赋值和解包，例如 `a, b = ...`
- 跨行模块调用

## 14. 常见错误对照

```python
# 错误：Python 布尔常量
set_run(True)

# 正确
set_run(true())

# 错误：列表下标
say(items[0])

# 正确
say(get_list_value("items", 0))

# 错误：列表模块的 name 参数传入了列表值
say(get_list_value(items, 0))

# 正确：传入列表名称
say(get_list_value("items", 0))

# 错误：range 步长尚不支持
for i in range(0, 10, 2):
    say(i)

# 正确
for i in range(0, 10):
    if i % 2 == 0:
        say(i)
```

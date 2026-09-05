# LittleAnt DSL 参考（给 AI 生成代码）

本文是 `AntDslConverter` 实际实现的语法说明。它描述的是一个“能编译成模块图的微型语言”，不是 Python 解释器；不要使用 Python 标准库、对象、属性、异常、文件或网络功能。模块名称和参数定义以 `ModuleRegistry` 为准。

## 最小模型

程序由一个或多个入口链组成。入口通常写成装饰器，入口下面的缩进代码是要执行的命令链：

```python
@ai_start
def init():
    say("ready")

@tick_start
def tick():
    if is_in_water():
        say("splash")
```

可用入口：`@ai_start`（初始化一次）、`@tick_start`（每 tick）、`@receive_goal` 和 `@goal_tick_start`（自定义目标）。装饰器必须是单独的裸名称；不要写 `@receive_goal("name")`。目标入口的目标名使用模块默认值 `custom_goal`（若要匹配其他名称，应在图编辑器中设置对应输入）：

```python
@receive_goal
def on_goal():
    say("goal started")
```

注意：当前转换器不会读取 `def` 的参数列表；函数参数不会绑定，函数调用也不可设置参数。普通函数定义会生成 `function_start`，调用时只按函数名查找；`greet(x=1)`、`greet(1)` 都会编译失败：

```python
def greet():
    say("hello")

@ai_start
def main():
    greet()                 # 等价于 call_function("greet")
```

## 词法和缩进

- 每行一个语句；空行和以 `#`（忽略前导空格）开头的行会被忽略。
- 缩进只按“空格数量”比较，要求子句的缩进严格大于父行；建议统一使用 4 个空格，不要混用 tab。
- 字符串可用单引号或双引号；逗号只有在字符串或括号嵌套之外才分隔参数。
- 源代码最长 65536 个字符，编译后模块最多 256 个。
- 语句末尾不要写分号。模块名、变量名区分大小写。

## 调用、参数和表达式

普通语句都是模块调用：

```python
say("hello")
move_to_xyz(10, 64, -2)
say(join_string_str("HP=", health()))
```

参数支持位置参数和 `name=value` 命名参数，二者可以混用；命名参数名必须是该模块真实的输入名：

```python
submit_background_goal(goal=better_float(), priority=2, move_flag=true())
use_container_xyz(x=1, y=64, z=2, put_in=true(), item="minecraft:stone", slot=0, amount=8)
```

未提供的参数使用模块默认值。只有 `CALL` 形式（标识符后跟括号）才会创建模块；未知名称会编译失败。

表达式不是 Python 运算式。支持的值表达式是模块调用、字面量和以下比较/逻辑语法：

```python
if get_variable("n") > 0 and not is_hurt():
    say("ok")
```

支持 `==`、`!=`、`>`、`<`、`>=`、`<=`，以及 `and`、`or`、前缀 `not`（也可写 `!`）。比较会编译为已有的数值比较模块；当两边是非数字文本时，运行时转换可能退回默认数值，因此不要把它当作完整的 Python 字符串比较。检查 reporter 是否返回空文本时，优先使用对应的布尔模块或先按 API 约定处理。 不支持 `+ - * / %`、括号改变比较优先级、`True/False/None`；算术必须写成 `add(a,b)`、`subtract(a,b)`、`multiply(a,b)`、`divide(a,b)`、`mod(a,b)`。布尔常量写 `true()`、`false()`。

## 变量：赋值语法与显式模块

赋值语句会自动转换为 `set_variable`，因此两种写法等价：

```python
target = find_block("minecraft:oak_log")
set_variable("target", find_block("minecraft:oak_log"))
```

在模块参数中，裸标识符现在会按该输入的类型自动读取黑板：NUMBER/TEXT/BOOLEAN 输入读取普通变量，LIST 输入读取同名列表。因此可以像 Python 一样直接写变量名；显式的 `get_variable`/`get_list` 仍然可用：

```python
n = 3
say(get_variable("n"))       # 显式写法
say(n)                        # 等价写法：输出 3
n = add(n, 1)                 # 裸 n 读取变量

pos = find_block("minecraft:oak_log")
move_to_blockpos(pos)         # 裸 pos 读取变量

items = []                    # 创建名为 items 的空列表（生成 new_list）
add_value("items", "minecraft:stone")
move_to_blockpos(items)       # LIST 输入会读取列表
```

变量和值在运行时以字符串保存；需要数字的输入会尝试解析数字，解析失败通常使用该输入的默认值。变量属于当前 ant 实例，并且在 ant 实例内没有作用域，但不是全局游戏变量。`a = []` 只负责创建空列表；列表元素仍使用 `add_value`、`set_list_kv` 等显式模块，列表索引从 0 开始：

```python
new_list("items")
add_value("items", "minecraft:stone")
set_list_kv("items", 0, "minecraft:dirt")
first = get_list_value("items", 0)
```

此外，由于列表存储机制，如果尝试用常量设置整列列表，如`set_list_list()`，应当写成引号中包含所有元素的形式，而不是一个一个元素列出：

```python
set_list_list("123,456,abc")  # 正确
set_list_list("123","456","abc")  # 错误
```

## 控制流（哪些像 Python，哪些不像）

### `if`

支持带缩进块和单行块：

```python
if has_item_in_inventory("minecraft:apple"):
    say("has apple")
if is_hurt(): say("ouch")
```

### `repeat`

原版 `repeat` 模块可以直接调用。转换器识别的形式是**无空格**的 `repeat(次数):`，次数可为数字或 reporter：

```python
repeat(3):
    jump()
repeat(get_variable("n")):
    say("again")
```

### `for ... in range(...)`

这是转换器提供的 Python 风格简写，实际会生成 `repeat`。支持 `range(stop)` 和 `range(start, stop)`；循环变量每次被写成从 0 开始的字符串索引。步长参数不会实现，不要写 `range(start, stop, step)`。

```python
for i in range(3):
    say(get_variable("i"))       # 0、1、2
for i in range(2, 5):
    say(get_variable("i"))       # 仍为 0、1、2；只执行 3 次
```

### `while`

支持块和单行形式；解释器最多执行 1000 次循环：

```python
while get_variable("n") > 0:
    n = subtract(get_variable("n"), 1)
```

`break()` 和 `continue()` 是控制模块，但只应在循环体中使用。现在支持标准的 `else:` 和 `elif ...:` 缩进块；转换器会把它们翻译为嵌套的 `if_else` 模块。`elif`/`else` 必须与对应的 `if` 保持相同缩进，暂不支持把 `elif` 或 `else` 写在单行 `if ...: command` 后面。

```python
if health() > 15:
    say("healthy")
elif health() > 5:
    say("wounded")
else:
    say("critical")
```

## 模块调用速查

以下名称均可直接作为 DSL 调用（参数顺序以 `ai-api-with-discription.md` 的模块章节为准）。同一功能通常有 `_xyz`（三个坐标参数）和 `_blockpos`（一个 `"x,y,z"` 参数）两个版本。

- 事件/目标：`submit_foreground_goal`、`submit_background_goal`、`receive_goal`、`goal_tick_start`、`finish_current_goal`、`finish_current_goal_delay`、`clear_goal`、`already_has_goal`、`already_has_goal_at_priority`。
- 行为：`move_to_xyz`、`move_to_blockpos`、`step_forward`、`look_at_xyz`、`look_at_blockpos`、`rotate`、`say`、`switch_inventory_slot`、`jump`、`set_run`、`set_crouching`、`set_pheromone`。
- 控制：`repeat`、`if`、`if_else`、`while`、`break`、`continue`。`elif`/`else` 会自动翻译为嵌套 `if_else`。
- 运算/判断：`add`、`subtract`、`multiply`、`divide`、`mod`、`absolute`、`random`、`greater_than`、`less_than`、`equal`、`not`、`and`、`or`、`true`、`false`、`join_string_list`、`join_string_str`、`contain_str`。
- 目标协议 reporter：`break_block_xyz`、`break_block_blockpos`、`set_block_xyz`、`set_block_blockpos`、`use_crafting_table_xyz`、`use_crafting_table_blockpos`、`use_inventory_crafting`、`better_float`、`use_container_xyz`、`use_container_blockpos`、`melee_attack`、`use_item`、`use_block_xyz`、`use_block_blockpos`、`interact_entity`。
- 感知：`health`、`food_level`、`x`、`y`、`z`、`pos`、`distance_to_xyz`、`distance_to_blockpos`、`get_block_xyz`、`get_block_blockpos`、`get_entity_at_xyz`、`get_entity_at_blockpos`、`get_entity_pos`、`has_item_in_inventory`、`get_item_in_inventory`、`get_item_count_in_inventory`、`time`、`is_hurt`、`is_on_fire`、`is_in_water`、`is_under_water`、`last_hurt_by_entity`、`find_block`、`find_entity`、`find_block_entity`、`find_pheromone`、`find_drop`、`get_surrounding_pheromone_types`、`has_item_in_container_xyz`、`has_item_in_container_blockpos`、`get_item_in_container_xyz`、`get_item_in_container_blockpos`、`get_item_count_in_container_xyz`、`get_item_count_in_container_blockpos`、`get_speed`。
- 变量/列表：`set_variable`、`get_variable`、`set_variable_permanent`、`new_list`、`get_list`、`add_list`、`add_value`、`set_list_kv`、`set_list_list`、`get_list_value`、`set_list_permanent`、`clear_list`。

## 目标提交的正确模式

目标 reporter 只构造字符串，不会等待目标完成；提交模块也是异步的。每 tick 提交前应检查是否已有同类目标：

```python
@tick_start
def seek_log():
    target = find_block("minecraft:oak_log")
    if target != "" and not already_has_goal(move_to_blockpos(target)):
        submit_foreground_goal(move_to_blockpos(target))
```

`find_*` 每次求值都会重新查询世界；保存到变量并复用。`vanilla:...` 字符串是调度器协议，不是可直接调用的 Python 函数。

## 常见错误

```python
# 错误：Python 运算符（裸变量读取本身是支持的）
n = n + 1

# 正确
n = add(get_variable("n"), 1)

# 注意：elif/else 必须使用缩进块，不能写在单行 if 后面
if is_hurt(): say("a")
elif is_in_water(): say("b")

# 正确：使用缩进块；它会自动生成嵌套 if_else
if is_hurt(): say("a")
if not is_hurt() and is_in_water(): say("b")
```

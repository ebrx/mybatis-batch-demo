# MyBatis Batch Update Demo (PostgreSQL)

本项目演示三种批量更新策略，并给出各自的适用场景、性能特征与局限性。

## 环境与依赖

- JDK 8
- Spring Boot 2.7.x
- MyBatis 3 + mybatis-spring-boot-starter
- PostgreSQL 11+ /  PostgreSQL 15.6

## 数据模型

表 `order_item`：
- 主键 `id` 为 `BIGINT`，默认 `nextval('order_item_id_seq')`
- 常用更新字段：`quantity`、`price`、`status`、`remark`

## 三种方式概览

1. **方式一：BatchExecutor（多条 SQL + JDBC batch）**
2. **方式二：CASE WHEN（单条 SQL）**
3. **方式三：ON CONFLICT Upsert（插入或更新）**

---

## 方式一：BatchExecutor（多条 SQL + JDBC batch）

**实现位置**
- `OrderItemService#batchUpdateByExecutor`
- `OrderItemMapper#updateById`

**机制**
- MyBatis `ExecutorType.BATCH`，循环调用单行 `UPDATE`。
- JDBC `addBatch()`，在 `flushStatements()` 或事务提交时统一发送。

**优点**
- 字段可选更新（`null` 字段不更新），灵活。
- SQL 简单，易读，数据库执行计划稳定。
- 更新行数大时更稳定，可分批提交，压力可控。

**缺点**
- 本质仍是 N 条 SQL，网络往返次数仍随行数增长。
- 需要正确处理 `BATCH` 与 `SIMPLE` 混用的事务边界，否则会触发：
  `Cannot change the ExecutorType when there is an existing transaction`。
- 批量插入/更新时 `getGeneratedKeys()` 不回填，不能依赖自动主键回写。

**性能特征**
- 单条 SQL 体积小，数据库解析快；
- 网络开销随记录数线性增长；
- 适合 **更新字段不固定** 或 **需要跳过 null 字段** 的场景。

**适用场景**
- 字段更新策略复杂、每行更新列不一致；
- 需要避免单条 SQL 过大；
- 允许更高网络开销换取执行稳定性。

---

## 方式二：CASE WHEN（单条 SQL）

**实现位置**
- `OrderItemMapper#batchUpdateByCaseWhen`

**机制**
- 生成一条 `UPDATE ... SET col = CASE id WHEN ... END`
- `WHERE id IN (...)`

**优点**
- 单次数据库交互，网络开销最低。
- SQL 只执行一次，适合高并发下减少连接占用。

**缺点**
- SQL 体积随批量大小增长，过大可能触发数据库包限制或计划解析变慢。
- 字段必须固定，无法灵活跳过 `null`（除非额外拼装逻辑）。
- PostgreSQL 对 `CASE` 推断类型敏感，需要显式类型转换（如 `status::smallint`）。

**性能特征**
- 小批量（几十～几百）优势明显；
- 批量过大时 SQL 解析时间、计划时间会显著上升；
- 受 `max_stack_depth`、`work_mem` 等限制。

**适用场景**
- 列固定、行数适中（建议 ≤ 1000）；
- 需要尽量减少网络往返；
- 数据库压力主要在 SQL 解析阶段可接受。

---

## 方式三：ON CONFLICT Upsert（插入或更新）

**实现位置**
- `OrderItemMapper#batchUpsert`

**机制**
- `INSERT ... ON CONFLICT (id) DO UPDATE`
- 本项目支持：
  - `id` 有值 → 更新
  - `id` 为空 → 使用 `DEFAULT` 生成主键并插入

**优点**
- 同时支持 “更新已存在” 与 “插入新行”。
- 语义清晰，适合同步、幂等写入场景。

**缺点**
- 只适合“按主键冲突”进行的 upsert 逻辑；
- 若批量中大量更新与插入混杂，锁争用更高；
- `id` 为空必须显式使用 `DEFAULT`（否则会触发 `NOT NULL` 约束）。

**性能特征**
- 插入与更新混合时性能取决于冲突比例；
- 高冲突率会增加索引竞争与锁等待；
- 单条 SQL，网络开销低。

**适用场景**
- 数据同步（如外部系统推送）；
- 需要幂等写入；
- 不想区分“新增/更新”逻辑的服务。

---

## 选择建议（简表）

| 场景 | 推荐方式 |
| ---- | -------- |
| 更新字段不固定、需跳过 null | 方式一 BatchExecutor |
| 批量行数中等、列固定、追求低网络开销 | 方式二 CASE WHEN |
| 同时可能插入和更新 | 方式三 Upsert |

---

## 测试注意事项

1. `BATCH` 与 `SIMPLE` 不能在同一事务切换执行。
   - 测试中建议不要在调用 `batchUpdateByExecutor` 的测试方法上再加 `@Transactional`。
2. `BATCH` 模式下 `useGeneratedKeys` 不会回填主键。
   - 若必须回填主键，请用 `SIMPLE` 执行 `INSERT`，或使用 `RETURNING id`。

---

## 运行测试

在本地数据库准备好 `demo_db` 后：

```bash
mvn -q -Dtest=OrderItemMapperTest test
```

---

## 目录结构

- `src/main/java/com/example/demo/service/OrderItemService.java`
- `src/main/resources/mapper/OrderItemMapper.xml`
- `src/test/java/com/example/demo/OrderItemMapperTest.java`


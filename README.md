# DB Template Optimized 使用文档

## 一、项目介绍

`db-template-spring-boot-starter`（`io.github.mydtx:db-template-spring-boot-starter:3.0.0`）是一个基于 Spring JDBC 的轻量级链式查询库，提供类似 MyBatis-Plus / ThinkPHP 的流畅 API，零运行时开销。

核心改进（相对旧版 `db-template`）：

- **消除代码重复**：提取公共 `QueryTemplate` 类，合并原 `TableTemplate.SqlBuilder` 与 `SingleTableTemplate`
- **线程安全**：每次查询创建独立实例，无共享状态问题
- **SQL 注入防护**：`validateIdentifier` 实现正则校验
- **`page()` / 聚合函数无副作用**：分页和聚合查询不修改当前实例状态
- **Lambda 缓存优化**：使用 `SerializedLambda` 签名作为缓存 key，跨实例缓存有效
- **软删除值可配置**：`deleted_at = 0` 不再是硬编码
- **Spring 构造器注入**：符合 Spring 最佳实践

---

## 二、Maven 依赖

```xml
<dependency>
    <groupId>io.github.mydtx</groupId>
    <artifactId>db-template-spring-boot-starter</artifactId>
    <version>3.0.0</version>
</dependency>

或本地安装后引用：

```bash
cd db-template-spring-boot-starter
mvn clean install -DskipTests
```

---

## 三、自动配置

Spring Boot 项目只需配置 DataSource，`DbTemplateAutoConfiguration` 会自动初始化 `Db` 类：

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db?useUnicode=true&characterEncoding=utf-8&useSSL=false
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

非 Spring Boot 项目手动初始化：

```java
@Configuration
public class DbConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        Db.init(jdbcTemplate);
    }
}
```

---

## 四、核心 API 预览

| 方法 | 说明 |
|------|------|
| `Db.table(Class)` | 通过实体类查询，支持多表 JOIN |
| `Db.table(String)` | 通过表名查询 |
| `Db.singleTable(Class)` | 类型安全的单表查询（支持 Lambda 字段名） |
| `Db.singleTable(String)` | 通过表名单表查询 |
| `extends BaseModel<T>` | 继承获得 insert / update / detail / delete / all |

---

## 五、基本使用方式

### 5.1 TableTemplate 方式（灵活查询）

通过实体类或表名获取 `QueryTemplate`，支持多表 JOIN 和灵活查询：

```java
import com.s365.dbtemplate.Db;
import com.s365.dbtemplate.QueryTemplate;

public class UserService {

    public User getUserById(Integer id) {
        return Db.table(User.class)
            .where("id", id)
            .first();
    }

    public List<User> getActiveUsers() {
        return Db.table(User.class)
            .where("status", 1)
            .orderByDesc("created_at")
            .list();
    }

    public List<User> searchByName(String keyword) {
        return Db.table(User.class)
            .whereLike("name", keyword)
            .list();
    }
}
```

### 5.2 SingleTableTemplate 方式（类型安全）

使用 Lambda 表达式引用字段，IDE 自动补全，编译期类型检查：

```java
import com.s365.dbtemplate.Db;
import com.s365.dbtemplate.SingleTableTemplate;

public class OrderService {

    public Order getOrderById(Integer id) {
        return Db.singleTable(Order.class)
            .where(Order::getId, id)
            .first();
    }

    public List<Order> getPendingOrders() {
        return Db.singleTable(Order.class)
            .where(Order::getStatus, 0)
            .orderByDesc(Order::getCreatedAt)
            .list();
    }
}
```

### 5.3 BaseModel 方式（快速 CRUD）

继承 `BaseModel<T>` 即刻获得标准 CRUD，无需编写任何模板代码：

```java
import com.s365.dbtemplate.BaseModel;

@Service
public class UserService extends BaseModel<User> {

    // 继承即获得 insert / update / detail / delete / all 方法
    public List<User> getUsers() {
        return all();
    }

    public User getUser(Integer id) {
        return detail(id);
    }

    // 自定义查询
    public List<User> getAdmins() {
        return getTable()
            .where("role", "admin")
            .list();
    }

    // 物理删除
    public Integer hardDelete(Integer id) {
        return trueDelete(id);
    }
}
```

---

## 六、Map 形式返回（灵活查询）

当查询字段不确定或多表 JOIN 时，可以使用 Map 形式返回结果：

```java
// 单条记录返回 Map
Map<String, Object> user = Db.table(User.class)
    .where("id", 1)
    .firstMap();
System.out.println(user.get("name"));

// 多条记录返回 List<Map>
List<Map<String, Object>> users = Db.table(User.class)
    .where("status", 1)
    .getMaps();
for (Map<String, Object> u : users) {
    System.out.println(u.get("name"));
}

// 分页返回 Map
PageResult<Map<String, Object>> result = Db.table(User.class)
    .where("status", 1)
    .pageMaps(1, 10);

// SingleTableTemplate 同样支持
Db.singleTable(User.class)
    .where(User::getStatus, 1)
    .firstMap();
```

---

## 七、SELECT 字段

```java
// 指定查询字段（字符串）
Db.table(User.class)
    .select("id, name, status")
    .list(User.class);

// 指定查询字段（Lambda）
Db.singleTable(User.class)
    .select(User::getId, User::getName)
    .list();

// 多表 JOIN 时指定
Db.table(Order.class)
    .select("orders.*, users.name as userName")
    .leftJoin(User.class, Order::getUserId, User::getId)
    .list(Order.class);
```

---

## 八、WHERE 条件

### 8.1 基本比较

```java
// 等于
.where("status", 1)
.where(User::getStatus, 1)

// 不等于
.whereNe("status", 0)
.whereNe(User::getStatus, 0)

// 小于
.whereLt("age", 18)
.whereLt(User::getAge, 18)

// 小于等于
.whereLe("age", 18)
.whereLe(User::getAge, 18)

// 大于
.whereGt("score", 60)
.whereGt(User::getScore, 60)

// 大于等于
.whereGe("score", 60)
.whereGe(User::getScore, 60)
```

### 8.2 LIKE 查询

```java
.whereLike("name", "张")
.whereLike(User::getName, "张")
```

自动包裹为 `%张%`，无需手动拼接百分号。

### 8.3 NULL 判断

```java
.whereIsNull("deleted_at")
.whereIsNull(User::getDeletedAt)

.whereNotNull("email")
.whereNotNull(User::getEmail)
```

### 8.4 IN / NOT IN

```java
.whereIn("id", Arrays.asList(1, 2, 3))
.whereIn(User::getId, Arrays.asList(1, 2, 3))

.whereNotIn("status", Arrays.asList(0, -1))
.whereNotIn(User::getStatus, Arrays.asList(0, -1))
```

### 8.5 FIND_IN_SET

```java
.whereFindInSet("tags", "java")
.whereFindInSet(User::getTags, "java")
-- 生成 SQL: FIND_IN_SET(?, tags)
```

### 8.6 OR 条件

```java
// 查找状态为 1 或 2 的用户
Db.table(User.class)
    .where("status", 1)
    .whereOr("status", 2)
    .list();
// SQL: WHERE status = ? OR status = ?

// OR + Like 组合
Db.table(User.class)
    .where("status", 1)
    .whereOrLike("name", "张")
    .list();
// SQL: WHERE status = ? OR name LIKE ?

// Lambda OR 条件
Db.singleTable(User.class)
    .where(User::getStatus, 1)
    .whereOr(User::getStatus, 2)
    .list();
```

### 8.7 多条件自由组合

```java
Db.table(User.class)
    .where("status", 1)
    .whereGt("age", 18)
    .whereLike("name", "张")
    .list();
```

---

## 九、排序与分页

### 9.1 排序

```java
// 降序
.orderByDesc("created_at")
.orderByDesc(User::getCreatedAt)

// 升序
.orderByAsc("sort")
.orderByAsc(User::getSort)

// 多字段排序
.orderByDesc("created_at")
.orderByAsc("id")
```

### 9.2 分页

```java
// page: 第几页（从 1 开始），size: 每页条数
PageResult<User> result = Db.table(User.class)
    .where("status", 1)
    .page(User.class, 1, 10);

System.out.println("总数: " + result.getTotal());
System.out.println("总页数: " + result.getLastPage());
System.out.println("当前页数据: " + result.getData());
```

`page()` 方法无副作用——内部独立构建 COUNT 和 SELECT 两条 SQL，不修改当前实例的任何状态。

### 9.3 限制条数

```java
// 只取前 5 条
.limit(5)

// 从第 10 条开始取 5 条
.limit(5, 10)
```

### 9.4 存在判断

```java
boolean exists = Db.table(User.class)
    .where("email", "test@example.com")
    .exists();

if (exists) {
    // 邮箱已存在
}
```

---

## 十、分组

```java
Db.table(Order.class)
    .where("status", 1)
    .groupBy("user_id")
    .list(Order.class);

// Lambda 分组
Db.singleTable(Order.class)
    .where(Order::getStatus, 1)
    .groupBy(Order::getUserId)
    .list();
```

---

## 十一、JOIN 查询

```java
// LEFT JOIN - 字符串方式
Db.table(Order.class)
    .leftJoin("user", "order.user_id", "user.id")
    .where("order.status", 1)
    .list();

// LEFT JOIN - Class + Lambda 方式
Db.table(Order.class)
    .leftJoin(User.class, Order::getUserId, User::getId)
    .select("orders.*, users.name")
    .where("orders.status", 1)
    .list(Order.class);

// RIGHT JOIN
Db.table(User.class)
    .rightJoin("orders", "user.id", "orders.user_id")
    .list();

// INNER JOIN
Db.table(Order.class)
    .innerJoin("products", "orders.product_id", "products.id")
    .list();
```

---

## 十二、聚合函数

```java
// 统计数量
Long count = Db.table(User.class).count();                    // COUNT(*)
Long activeCount = Db.table(User.class).where("status", 1).count();
Long nameCount = Db.table(User.class).count("name");          // COUNT(name)
Long lambdaCount = Db.table(User.class).count(User::getName);

// 最大值（空结果返回 null）
Integer maxAge = Db.table(User.class).max("age");
Integer maxAge2 = Db.table(User.class).max(User::getAge);

// 最小值（空结果返回 null）
Integer minAge = Db.table(User.class).min("age");

// 平均值（空结果返回 null）
Double avgScore = Db.table(User.class).avg("score");

// 求和（空结果返回 null）
Double totalAmount = Db.table(Order.class).sum("amount");
```

> ⚠️ 注意：聚合方法不会修改当前实例的 `selectString`，调用聚合方法后再执行查询不受影响。

---

## 十三、增删改

### 13.1 新增

```java
// 通过对象插入（返回自增 ID）
User user = new User();
user.setName("张三");
user.setAge(20);
int id = Db.table(User.class).insert(user);

// 通过 Map 插入
Map<String, Object> values = new HashMap<>();
values.put("name", "李四");
values.put("age", 25);
int id = Db.table(User.class).add(values);
```

> 默认自动填充 `created_at` 和 `updated_at` 为当前时间戳。

### 13.2 更新

```java
// 通过对象更新（对象需包含 id）
User user = new User();
user.setId(1);
user.setName("张三更新");
int rows = Db.table(User.class).update(user);

// 通过 Map 更新（带 where 条件）
int rows = Db.table(User.class)
    .where("id", 1)
    .update(new HashMap<String, Object>() {{
        put("name", "张三更新");
    }});
```

> 更新默认自动填充 `updated_at`。

### 13.3 删除

```java
// 软删除（默认，设置 deleted_at = 当前时间戳）
int rows = Db.table(User.class)
    .where("id", 1)
    .delete();

// 物理删除（跳过软删除）
int rows = Db.table(User.class)
    .unUseSoftDelete()
    .where("id", 1)
    .delete();
```

### 13.4 自增/自减

```java
// 字段自增
Db.table(User.class)
    .where("id", 1)
    .increment("login_count");
Db.table(User.class)
    .where("id", 1)
    .increment(User::getLoginCount);

// 字段自减
Db.table(User.class)
    .where("id", 1)
    .decrement("stock");
Db.table(User.class)
    .where("id", 1)
    .decrement(User::getStock);
```

---

## 十四、高级配置

### 14.1 禁用自动填充时间

```java
Db.table(User.class)
    .unUseAutoFill()
    .insert(user);
// 不会自动设置 created_at 和 updated_at
```

### 14.2 禁用软删除

```java
// 查询时不过滤已删除记录
Db.table(User.class)
    .unUseSoftDelete()
    .where("status", 1)
    .list();

// 物理删除
Db.table(User.class)
    .unUseSoftDelete()
    .where("id", 1)
    .delete();
```

### 14.3 自定义软删除标记值

默认软删除标记值 `deleted_at = 0` 表示未删除，可自定义：

```java
// 使用 -1 作为未删除标记
Db.table(User.class)
    .setSoftDeleteValue(-1L)
    .where("status", 1)
    .list();

// 也支持 SingleTableTemplate
Db.singleTable(User.class)
    .setSoftDeleteValue(-1L)
    .list();
```

### 14.4 自定义时间字段名

```java
Db.table(User.class)
    .setInsertTimeField("create_time")
    .setUpdateTimeField("update_time")
    .insert(user);
```

### 14.5 行锁

```java
// 排他锁（适用于扣库存、扣余额等场景）
Db.table(User.class)
    .where("id", 1)
    .lockForUpdate()
    .first();

// 共享锁
Db.table(User.class)
    .where("id", 1)
    .lockForShare()
    .first();
```

### 14.6 开关软删除/自动填充（入口处）

`Db.table()` 和 `Db.singleTable()` 支持在入口处直接配置开关：

```java
// 禁用软删除
Db.table(User.class, true)

// 禁用软删除 + 禁用自动填充
Db.table(User.class, true, true)

// 同 singleTable
Db.singleTable(User.class, true)
Db.singleTable(User.class, true, true)
```

参数顺序：`(Class, unUseSoftDelete, unUseAutoFill)`。

---

## 十五、完整示例

### 实体类

```java
import lombok.Data;

@Data
public class User {
    private Integer id;
    private String name;
    private Integer age;
    private Integer status;
    private Integer loginCount;
    private String tags;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}

@Data
public class Order {
    private Integer id;
    private Integer userId;
    private Double amount;
    private Integer status;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
```

### Service 示例

```java
@Service
public class UserService extends BaseModel<User> {

    /** 登录：增加登录次数 */
    public void loginSuccess(Integer userId) {
        getTable()
            .where("id", userId)
            .increment(User::getLoginCount);
    }

    /** 分页查询活跃用户 */
    public PageResult<User> pageActiveUsers(int page, int size) {
        return getTable()
            .where(User::getStatus, 1)
            .orderByDesc(User::getCreatedAt)
            .page(User.class, page, size);
    }

    /** 按标签搜索用户（FIND_IN_SET） */
    public List<User> searchByTag(String tag) {
        return getTable()
            .whereFindInSet(User::getTags, tag)
            .list();
    }

    /** 物理删除 */
    public void hardDelete(Integer id) {
        getTable(false)
            .where("id", id)
            .delete();
    }
}
```

---

## 十六、与旧版本的差异对比

| 功能 | 旧版本 | 优化版本 |
|------|--------|----------|
| 入口 | `Db.table(User.class)` 返回内部类 | `Db.table(User.class)` 返回 `QueryTemplate<T>` |
| 线程安全 | 实例状态可能被多线程篡改 | 每次查询新实例，天然线程安全 |
| 分页 | `page()` 修改 `selectString` 有副作用 | `page()` 内部独立构建 count SQL，无副作用 |
| 聚合函数 | `count()`/`max()` 修改 `selectString` | save/restore 模式，不影响后续查询 |
| SQL 注入 | `validateIdentifier` 为空实现 | 正则校验标识符 |
| Lambda 缓存 | 以 lambda 实例为 key，缓存失效 | 以 `SerializedLambda` 签名为 key |
| 软删除值 | 硬编码 `deleted_at = 0` | 可通过 `setSoftDeleteValue()` 自定义 |
| 类型安全 | 聚合返回 `(T) Double.class`，空结果抛异常 | 使用 `Number.class`，空结果返回 null |
| Spring 注入 | `@Autowired` 字段注入 | 构造器注入 |
| 单元测试 | 无 | 49 个测试用例覆盖全部核心功能 |

---

## 类结构总览

```
Db (静态入口)
 ├── table()         → TableTemplate → QueryTemplate<T>
 ├── singleTable()   → SingleTableTemplate<T> (委托 QueryTemplate)
 └── init()          初始化 JdbcTemplate 连接

QueryTemplate<T> (核心查询构建器 + 执行器)
 ├── 继承 BaseDbTemplate (工具方法、SQL校验、对象映射)
 ├── WHERE 条件: where / whereNe / whereLt / whereLe / whereGt / whereGe
 │              whereLike / whereIsNull / whereNotNull / whereFindInSet
 │              whereIn / whereNotIn / whereOr / whereOrNe / whereOrLike
 ├── 排序: orderByDesc / orderByAsc
 ├── 分组: groupBy
 ├── 分页: limit / page / pageMaps
 ├── JOIN: leftJoin / rightJoin / innerJoin
 ├── 聚合: count / max / min / avg / sum
 ├── 查询: first / one / get / list / exists / firstMap / getMaps
 ├── 增删改: insert / add / update / delete
 ├── 自增/减: increment / decrement
 ├── 锁: lockForUpdate / lockForShare
 └── 配置: unUseAutoFill / unUseSoftDelete / setSoftDeleteValue
           setInsertTimeField / setUpdateTimeField

BaseModel<T> (快速 CRUD 基类)
 └── insert / update / detail / delete / all / trueDelete / getTable

PageResult<T> (分页结果)
 └── total / page / pageSize / data / lastPage
```

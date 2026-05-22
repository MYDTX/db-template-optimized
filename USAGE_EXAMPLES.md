# DB Template Optimized 使用文档

## 一、项目介绍

`db-template-optimized` 是对原 `db-template` 的优化版本，核心改进：

- **消除代码重复**：提取公共 `QueryTemplate` 类，合并原 `TableTemplate.SqlBuilder` 与 `SingleTableTemplate`
- **线程安全**：每次查询创建独立实例，无共享状态问题
- **SQL 注入防护**：`validateIdentifier` 实现正则校验
- **`page()` 无副作用**：分页查询不修改当前实例状态
- **Lambda 缓存优化**：使用 `SerializedLambda` 签名作为缓存 key，避免缓存失效
- **Spring 构造器注入**：符合 Spring 最佳实践

---

## 二、Maven 依赖

```xml
<dependency>
    <groupId>io.github.mydtx</groupId>
    <artifactId>db-template-optimized</artifactId>
    <version>2.0.0</version>
</dependency>
```

或本地安装后引用：

```bash
cd db-template-optimized
mvn clean install
```

---

## 三、自动配置

项目依赖 Spring Boot，只要存在 `JdbcTemplate` Bean，`DbTemplateAutoConfiguration` 会自动初始化：

```java
// application.yml 示例
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test
    username: root
    password: 123456
```

如需手动初始化：

```java
@Autowired
public void init(JdbcTemplate jdbcTemplate) {
    Db.init(jdbcTemplate);
}
```

---

## 四、基本使用方式

### 4.1 TableTemplate 方式（灵活查询）

通过实体类或表名获取 `QueryTemplate`，支持多表 JOIN 和灵活查询：n

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
            .get();
    }

    public List<User> searchByName(String keyword) {
        return Db.table(User.class)
            .whereLike("name", keyword)
            .get();
    }
}
```

### 4.2 SingleTableTemplate 方式（类型安全）

使用 Lambda 表达式，IDE 自动补全字段名：

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

### 4.3 BaseModel 方式（快速 CRUD）

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
}
```

---

## 四、Map 形式返回（灵活查询）

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

## 五、WHERE 条件

### 5.1 基本比较

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

### 5.2 LIKE 查询

```java
.whereLike("name", "张")
.whereLike(User::getName, "张")
```

### 5.3 NULL 判断

```java
.whereIsNull("deleted_at")
.whereIsNull(User::getDeletedAt)

.whereNotNull("email")
.whereNotNull(User::getEmail)
```

### 5.4 IN / NOT IN

```java
.whereIn("id", Arrays.asList(1, 2, 3))
.whereIn(User::getId, Arrays.asList(1, 2, 3))

.whereNotIn("status", Arrays.asList(0, -1))
.whereNotIn(User::getStatus, Arrays.asList(0, -1))
```

### 5.5 FIND_IN_SET

```java
.whereFindInSet("tags", "java")
.whereFindInSet(User::getTags, "java")
```

### 5.6 OR 条件

```java
// 查找状态为 1 或 2 的用户（OR 连接）
Db.table(User.class)
    .where("status", 1)
    .whereOr("status", 2)
    .get();
// 生成 SQL: WHERE status = ? OR status = ?

// 查找名字为张三或李四的用户
Db.table(User.class)
    .where("name", "张三")
    .whereOr("name", "李四")
    .get();
// 生成 SQL: WHERE name = ? OR name = ?

// Lambda OR 条件
Db.singleTable(User.class)
    .where(User::getStatus, 1)
    .whereOr(User::getStatus, 2)
    .get();

// OR + Like 组合
Db.table(User.class)
    .where("status", 1)
    .whereOrLike("name", "张")
    .get();
// 生成 SQL: WHERE status = ? OR name LIKE ?

// 多 OR 条件
Db.table(User.class)
    .where("status", 1)
    .whereOr("status", 2)
    .whereOr("status", 3)
    .get();
// 生成 SQL: WHERE status = ? OR status = ? OR status = ?
```

### 5.7 多条件组合

```java
Db.table(User.class)
    .where("status", 1)
    .whereGt("age", 18)
    .whereLike("name", "张")
    .get();
```

---

## 六、排序与分页

### 6.1 排序

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

### 6.2 分页

```java
// page: 第几页（从 1 开始），size: 每页条数
PageResult<User> result = Db.table(User.class)
    .where("status", 1)
    .page(User.class, 1, 10);

System.out.println("总数: " + result.getTotal());
System.out.println("总页数: " + result.getLastPage());
System.out.println("当前页数据: " + result.getData());
```

### 6.3 限制条数

```java
// 只取前 5 条
.limit(5)

// 从第 10 条开始取 5 条
.limit(5, 10)
```

---

## 七、JOIN 查询

```java
// 左连接
Db.table(Order.class)
    .leftJoin("user", "order.user_id", "user.id")
    .where("order.status", 1)
    .get();

// 使用 Class + Lambda
Db.table(Order.class)
    .leftJoin(User.class, Order::getUserId, User::getId)
    .where("order.status", 1)
    .get();
```

---

## 八、聚合函数

```java
// 统计数量
Long count = Db.table(User.class).count();
Long activeCount = Db.table(User.class).where("status", 1).count();
Long nameCount = Db.table(User.class).count("name");
Long lambdaCount = Db.table(User.class).count(User::getName);

// 最大值
Integer maxAge = Db.table(User.class).max("age");
Integer maxAge2 = Db.table(User.class).max(User::getAge);

// 最小值
Integer minAge = Db.table(User.class).min("age");

// 平均值
Double avgScore = Db.table(User.class).avg("score");

// 求和
Double totalAmount = Db.table(Order.class).sum("amount");
```

---

## 九、增删改

### 9.1 新增

```java
// 通过对象插入
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

### 9.2 更新

```java
// 通过对象更新（需包含 id）
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

### 9.3 删除

```java
// 软删除（默认）
int rows = Db.table(User.class)
    .where("id", 1)
    .delete();

// 物理删除（禁用软删除）
int rows = Db.table(User.class)
    .unUseSoftDelete()
    .where("id", 1)
    .delete();
```

### 9.4 自增/自减

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

## 十、高级配置

### 10.1 禁用自动填充时间

```java
Db.table(User.class)
    .unUseAutoFill()
    .insert(user);
```

### 10.2 禁用软删除

```java
Db.table(User.class)
    .unUseSoftDelete()
    .where("id", 1)
    .delete();
```

### 10.3 自定义时间字段名

```java
Db.table(User.class)
    .setInsertTimeField("create_time")
    .setUpdateTimeField("update_time")
    .insert(user);
```

### 10.4 行锁

```java
Db.table(User.class)
    .where("id", 1)
    .lockForUpdate()
    .first();
```

---

## 十一、与旧版本的主要差异

| 功能 | 旧版本 | 优化版本 |
|------|--------|----------|
| 入口 | `Db.table(User.class)` 返回内部类 | `Db.table(User.class)` 返回 `QueryTemplate<T>` |
| 线程安全 | 实例状态可能被多线程篡改 | 每次查询新实例，天然线程安全 |
| 分页 | `page()` 修改 `selectString` 有副作用 | `page()` 内部独立构建 count SQL，无副作用 |
| SQL 注入 | `validateIdentifier` 为空实现 | 正则校验标识符 |
| Lambda 缓存 | 以 lambda 实例为 key，缓存失效 | 以 `SerializedLambda` 签名为 key |
| Spring 注入 | `@Autowired` 字段注入 | 构造器注入 |

---

## 十二、实体类示例

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
```

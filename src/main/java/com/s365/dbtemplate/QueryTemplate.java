package com.s365.dbtemplate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心查询构建器，合并了原 TableTemplate.SqlBuilder 和 SingleTableTemplate 的公共逻辑。
 * 每次查询创建新实例，天然线程安全。
 */
public class QueryTemplate<T> extends BaseDbTemplate {

    // 查询状态
    private final List<String> joinList = new ArrayList<>();
    private final List<String> orderByList = new ArrayList<>();
    private final List<String> groupByList = new ArrayList<>();
    private final List<WhereCondition> whereConditions = new ArrayList<>();
    private final List<WhereInCondition> whereInConditions = new ArrayList<>();
    private final Map<String, Integer> limitMap = new HashMap<>();

    private String selectString;
    private String tableName;
    private Class<T> entityClass;
    private Boolean autoIncrement = true;

    // 配置
    private Boolean autoFill = true;
    private String updateTimeField = "updated_at";
    private String insertTimeField = "created_at";
    private Boolean softDelete = true;
    private Object softDeleteValue = 0;
    private String lockShare = " FOR SHARE ";
    private String lockUpdate = " FOR UPDATE ";
    private String lock = null;

    public QueryTemplate(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    // ==================== 配置方法（链式调用） ====================

    public QueryTemplate<T> table(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public QueryTemplate<T> table(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.tableName = ClassNameUtils.getClassNameAsUnderscore(entityClass);
        return this;
    }

    public QueryTemplate<T> unUseAutoFill() {
        this.autoFill = false;
        return this;
    }

    public QueryTemplate<T> setUpdateTimeField(String updateTimeField) {
        this.updateTimeField = updateTimeField;
        return this;
    }

    public QueryTemplate<T> setInsertTimeField(String insertTimeField) {
        this.insertTimeField = insertTimeField;
        return this;
    }

    public QueryTemplate<T> lockForUpdate() {
        this.lock = lockUpdate;
        return this;
    }

    public QueryTemplate<T> lockForShare() {
        this.lock = lockShare;
        return this;
    }

    public QueryTemplate<T> unUseSoftDelete() {
        this.softDelete = false;
        return this;
    }

    public QueryTemplate<T> setSoftDeleteValue(Object softDeleteValue) {
        this.softDeleteValue = softDeleteValue;
        return this;
    }

    // ==================== WHERE 条件 ====================

    public <V> QueryTemplate<T> where(String key, V value) {
        whereConditions.add(new WhereCondition(key, "=", value, getUuid()));
        return this;
    }

    public <K, V> QueryTemplate<T> where(StringFunction<K> field, V value) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, "=", value, getUuid()));
        return this;
    }

    public <K, V> QueryTemplate<T> whereNe(StringFunction<K> field, V value) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, "<>", value, getUuid()));
        return this;
    }

    public <V> QueryTemplate<T> whereNe(String key, V value) {
        whereConditions.add(new WhereCondition(key, "<>", value, getUuid()));
        return this;
    }

    public <K, V> QueryTemplate<T> whereLt(StringFunction<K> field, V value) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, "<", value, getUuid()));
        return this;
    }

    public <V> QueryTemplate<T> whereLt(String key, V value) {
        whereConditions.add(new WhereCondition(key, "<", value, getUuid()));
        return this;
    }

    public <K, V> QueryTemplate<T> whereLe(StringFunction<K> field, V value) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, "<=", value, getUuid()));
        return this;
    }

    public <V> QueryTemplate<T> whereLe(String key, V value) {
        whereConditions.add(new WhereCondition(key, "<=", value, getUuid()));
        return this;
    }

    public <K, V> QueryTemplate<T> whereGt(StringFunction<K> field, V value) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, ">", value, getUuid()));
        return this;
    }

    public <V> QueryTemplate<T> whereGt(String key, V value) {
        whereConditions.add(new WhereCondition(key, ">", value, getUuid()));
        return this;
    }

    public <K, V> QueryTemplate<T> whereGe(StringFunction<K> field, V value) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, ">=", value, getUuid()));
        return this;
    }

    public <V> QueryTemplate<T> whereGe(String key, V value) {
        whereConditions.add(new WhereCondition(key, ">=", value, getUuid()));
        return this;
    }

    public <K, V> QueryTemplate<T> whereLike(StringFunction<K> field, V value) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, "like", value, getUuid(), true));
        return this;
    }

    public <V> QueryTemplate<T> whereLike(String key, V value) {
        whereConditions.add(new WhereCondition(key, "like", value, getUuid(), true));
        return this;
    }

    public <V> QueryTemplate<T> whereIsNull(StringFunction<V> field) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, "is null", null, null));
        return this;
    }

    public QueryTemplate<T> whereIsNull(String key) {
        whereConditions.add(new WhereCondition(key, "is null", null, null));
        return this;
    }

    public QueryTemplate<T> whereNotNull(String key) {
        whereConditions.add(new WhereCondition(key, "is not null", null, null));
        return this;
    }

    public <V> QueryTemplate<T> whereNotNull(StringFunction<V> field) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, "is not null", null, null));
        return this;
    }

    public <K, V> QueryTemplate<T> whereFindInSet(StringFunction<K> filed, V value) {
        String key = FieldNameUtils.getSimpleFieldName(filed);
        whereConditions.add(new WhereCondition(key, "FIND_IN_SET", value, key));
        return this;
    }

    public <V> QueryTemplate<T> whereFindInSet(String key, V value) {
        whereConditions.add(new WhereCondition(key, "FIND_IN_SET", value, key));
        return this;
    }

    public <K, V> QueryTemplate<T> whereIn(StringFunction<K> filed, List<V> values) {
        String key = FieldNameUtils.getSimpleFieldName(filed);
        whereInConditions.add(new WhereInCondition(key, "in", values, getUuid()));
        return this;
    }

    public <V> QueryTemplate<T> whereIn(String key, List<V> values) {
        whereInConditions.add(new WhereInCondition(key, "in", values, getUuid()));
        return this;
    }

    public <K, V> QueryTemplate<T> whereNotIn(StringFunction<K> filed, List<V> values) {
        String key = FieldNameUtils.getSimpleFieldName(filed);
        whereInConditions.add(new WhereInCondition(key, "not in", values, getUuid()));
        return this;
    }

    public <V> QueryTemplate<T> whereNotIn(String key, List<V> values) {
        whereInConditions.add(new WhereInCondition(key, "not in", values, getUuid()));
        return this;
    }

    // ==================== WHERE OR 条件 ====================

    public <V> QueryTemplate<T> whereOr(String key, V value) {
        whereConditions.add(new WhereCondition(key, "=", value, getUuid(), false, true));
        return this;
    }

    public <K, V> QueryTemplate<T> whereOr(StringFunction<K> field, V value) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, "=", value, getUuid(), false, true));
        return this;
    }

    public <K, V> QueryTemplate<T> whereOrNe(StringFunction<K> field, V value) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, "<>", value, getUuid(), false, true));
        return this;
    }

    public <V> QueryTemplate<T> whereOrNe(String key, V value) {
        whereConditions.add(new WhereCondition(key, "<>", value, getUuid(), false, true));
        return this;
    }

    public <K, V> QueryTemplate<T> whereOrLike(StringFunction<K> field, V value) {
        String key = FieldNameUtils.getSimpleFieldName(field);
        whereConditions.add(new WhereCondition(key, "like", value, getUuid(), true, true));
        return this;
    }

    public <V> QueryTemplate<T> whereOrLike(String key, V value) {
        whereConditions.add(new WhereCondition(key, "like", value, getUuid(), true, true));
        return this;
    }

    // ==================== SELECT / JOIN / ORDER / GROUP / LIMIT ====================

    public QueryTemplate<T> select(String field) {
        this.selectString = field;
        return this;
    }

    public <K> QueryTemplate<T> select(StringFunction<K>... key) {
        try {
            this.selectString = Arrays.stream(key).map(FieldNameUtils::getSimpleFieldName).collect(Collectors.joining(","));
        } catch (IllegalArgumentException e) {
            this.selectString = "*";
        }
        return this;
    }

    private QueryTemplate<T> join(String type, String TABLE_NAME, String localField, String foreignField) {
        this.joinList.add(" " + type + " JOIN " + TABLE_NAME + " ON " + localField + "=" + foreignField);
        return this;
    }

    public <R, L, F> QueryTemplate<T> leftJoin(Class<R> TABLE_NAME, StringFunction<L> localField, StringFunction<F> foreignField) {
        return this.join("LEFT", ClassNameUtils.getClassNameAsUnderscore(TABLE_NAME), FieldNameUtils.getSimpleFieldName(localField), FieldNameUtils.getSimpleFieldName(foreignField));
    }

    public QueryTemplate<T> leftJoin(String TABLE_NAME, String localField, String foreignField) {
        return this.join("LEFT", TABLE_NAME, localField, foreignField);
    }

    public QueryTemplate<T> rightJoin(String TABLE_NAME, String localField, String foreignField) {
        return this.join("RIGHT", TABLE_NAME, localField, foreignField);
    }

    public <R, L, F> QueryTemplate<T> rightJoin(Class<R> TABLE_NAME, StringFunction<L> localField, StringFunction<F> foreignField) {
        return this.join("RIGHT", ClassNameUtils.getClassNameAsUnderscore(TABLE_NAME), FieldNameUtils.getSimpleFieldName(localField), FieldNameUtils.getSimpleFieldName(foreignField));
    }

    public QueryTemplate<T> innerJoin(String TABLE_NAME, String localField, String foreignField) {
        return this.join("INNER", TABLE_NAME, localField, foreignField);
    }

    public QueryTemplate<T> orderByDesc(String field) {
        this.orderByList.add(field + " DESC");
        return this;
    }

    public <K> QueryTemplate<T> orderByDesc(StringFunction<K> field) {
        this.orderByList.add(FieldNameUtils.getSimpleFieldName(field) + " DESC");
        return this;
    }

    public QueryTemplate<T> orderByAsc(String field) {
        this.orderByList.add(field + " ASC");
        return this;
    }

    public <K> QueryTemplate<T> orderByAsc(StringFunction<K> field) {
        this.orderByList.add(FieldNameUtils.getSimpleFieldName(field) + " ASC");
        return this;
    }

    public QueryTemplate<T> groupBy(String field) {
        this.groupByList.add(field);
        return this;
    }

    public <K> QueryTemplate<T> groupBy(StringFunction<K> field) {
        this.groupByList.add(FieldNameUtils.getSimpleFieldName(field));
        return this;
    }

    public QueryTemplate<T> limit(int size) {
        return this.limit(size, 0);
    }

    public QueryTemplate<T> limit(int size, int offset) {
        this.limitMap.clear();
        this.limitMap.put("size", size);
        this.limitMap.put("offset", offset);
        return this;
    }

    // ==================== SQL 构建 ====================

    String buildWhereString() {
        StringBuilder whereSql = new StringBuilder();
        for (WhereCondition condition : whereConditions) {
            if (whereSql.length() == 0) {
                whereSql.append(" where ");
            } else if (condition.orCondition) {
                whereSql.append(" or ");
            } else {
                whereSql.append(" and ");
            }
            if ("is null".equals(condition.operator) || "is not null".equals(condition.operator)) {
                whereSql.append(condition.field).append(" ").append(condition.operator);
            } else if ("FIND_IN_SET".equals(condition.operator)) {
                whereSql.append(condition.operator).append("(?, ").append(condition.field).append(")");
            } else {
                whereSql.append(condition.field).append(" ").append(condition.operator).append(" ?");
            }
        }
        for (WhereInCondition condition : whereInConditions) {
            if (whereSql.length() == 0) {
                whereSql.append(" where ");
            } else {
                whereSql.append(" and ");
            }
            String placeholders = String.join(",", Collections.nCopies(condition.values.size(), "?"));
            whereSql.append(condition.field).append(" ").append(condition.operator)
                    .append(" (").append(placeholders).append(")");
        }
        // 软删除条件
        if (softDelete && !hasDeletedAtCondition()) {
            if (whereSql.length() == 0) {
                whereSql.append(" where ").append(tableName).append(".deleted_at = ?");
            } else {
                whereSql.append(" and ").append(tableName).append(".deleted_at = ?");
            }
        }
        if (whereSql.length() == 0) {
            whereSql.append(" where 1=1");
        }
        return whereSql.toString();
    }

    private List<Object> buildWhereValues() {
        List<Object> values = new ArrayList<>();
        for (WhereCondition condition : whereConditions) {
            if (condition.value != null) {
                if (condition.likeOperation) {
                    values.add("%" + condition.value + "%");
                } else if (!"is null".equals(condition.operator) && !"is not null".equals(condition.operator)) {
                    values.add(condition.value);
                }
            } else if ("FIND_IN_SET".equals(condition.operator) && condition.value != null) {
                values.add(condition.value);
            }
        }
        for (WhereInCondition condition : whereInConditions) {
            values.addAll(condition.values);
        }
        // 软删除参数
        if (softDelete && !hasDeletedAtCondition()) {
            values.add(softDeleteValue);
        }
        return values;
    }

    private boolean hasDeletedAtCondition() {
        for (WhereCondition condition : whereConditions) {
            if (condition.field != null && condition.field.contains("deleted_at")) {
                return true;
            }
        }
        return false;
    }

    String buildSelectSql() {
        if (selectString == null || selectString.isEmpty()) {
            selectString = tableName + ".*";
        }
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(selectString)
                .append(" FROM ")
                .append(tableName)
                .append(getJoinString())
                .append(buildWhereString())
                .append(getGroupString())
                .append(getOrderString())
                .append(getLimitString())
                .append(getLockString());
        return sql.toString();
    }

    private String buildCountSql() {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ")
                .append(tableName)
                .append(getJoinString())
                .append(buildWhereString());
        return sql.toString();
    }

    private String getJoinString() {
        return joinList.isEmpty() ? "" : String.join(" ", joinList);
    }

    private String getOrderString() {
        return orderByList.isEmpty() ? "" : " ORDER BY " + String.join(", ", orderByList);
    }

    private String getGroupString() {
        return groupByList.isEmpty() ? "" : " GROUP BY " + String.join(", ", groupByList);
    }

    private String getLimitString() {
        if (limitMap == null || limitMap.isEmpty()) {
            return "";
        }
        return " LIMIT " + limitMap.get("offset") + "," + limitMap.get("size");
    }

    private String getLockString() {
        return lock == null ? "" : lock;
    }

    // ==================== 查询执行 ====================

    public Boolean exists() {
        String sql = buildCountSql();
        Long count = jdbcTemplate.queryForObject(sql, buildWhereValues().toArray(), Long.class);
        return count != null && count > 0;
    }

    public <R> R first(Class<R> entityClass) {
        limit(1);
        List<R> results = jdbcTemplate.query(buildSelectSql(), buildWhereValues().toArray(), getRowMapper(entityClass));
        return results.isEmpty() ? null : results.get(0);
    }

    public <R> R one(Class<R> entityClass) {
        return first(entityClass);
    }

    public <R> List<R> get(Class<R> entityClass) {
        return jdbcTemplate.query(buildSelectSql(), buildWhereValues().toArray(), getRowMapper(entityClass));
    }

    public <R> List<R> list(Class<R> entityClass) {
        return get(entityClass);
    }

    /**
     * 查询单条记录，以 Map 形式返回
     */
    public Map<String, Object> firstMap() {
        limit(1);
        List<Map<String, Object>> results = jdbcTemplate.query(buildSelectSql(), buildWhereValues().toArray(), new org.springframework.jdbc.core.ColumnMapRowMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 查询多条记录，以 List<Map> 形式返回
     */
    public List<Map<String, Object>> getMaps() {
        return jdbcTemplate.query(buildSelectSql(), buildWhereValues().toArray(), new org.springframework.jdbc.core.ColumnMapRowMapper());
    }

    /**
     * 查询多条记录，以 List<Map> 形式返回（别名）
     */
    public List<Map<String, Object>> listMaps() {
        return getMaps();
    }

    /**
     * 分页查询，以 Map 形式返回
     */
    public PageResult<Map<String, Object>> pageMaps(int page, int size) {
        String countSql = buildCountSql();
        Number total = jdbcTemplate.queryForObject(countSql, buildWhereValues().toArray(), Number.class);
        long totalCount = total != null ? total.longValue() : 0L;

        limit(size, (page - 1) * size);
        List<Map<String, Object>> list = getMaps();
        return new PageResult<>(totalCount, page, size, list);
    }

    public <R> PageResult<R> page(Class<R> entityClass, int page, int size) {
        // 1. 先查总数（不修改当前 selectString）
        String countSql = buildCountSql();
        Number total = jdbcTemplate.queryForObject(countSql, buildWhereValues().toArray(), Number.class);
        long totalCount = total != null ? total.longValue() : 0L;

        // 2. 再查分页数据
        limit(size, (page - 1) * size);
        List<R> list = get(entityClass);
        return new PageResult<>(totalCount, page, size, list);
    }

    // ==================== 聚合函数 ====================

    public Long count(String field) {
        String originalSelect = this.selectString;
        try {
            String selectExpr = (field == null || field.isEmpty()) ? "COUNT(*)" : "COUNT(" + validateIdentifier(field) + ")";
            this.selectString = selectExpr;
            Number result = jdbcTemplate.queryForObject(buildSelectSql(), buildWhereValues().toArray(), Number.class);
            return result != null ? result.longValue() : 0L;
        } finally {
            this.selectString = originalSelect;
        }
    }

    public Long count() {
        return count("");
    }

    public <K> Long count(StringFunction<K> field) {
        String fieldName = FieldNameUtils.getSimpleFieldName(field);
        return count(fieldName);
    }

    public <T extends Number> T max(String field) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty for max operation.");
        }
        String originalSelect = this.selectString;
        try {
            this.selectString = "MAX(" + validateIdentifier(field) + ")";
            Number result = jdbcTemplate.queryForObject(buildSelectSql(), buildWhereValues().toArray(), Number.class);
            return result != null ? (T) result : null;
        } finally {
            this.selectString = originalSelect;
        }
    }

    public <K, T extends Number> T max(StringFunction<K> field) {
        String fieldName = FieldNameUtils.getSimpleFieldName(field);
        return max(fieldName);
    }

    public <T extends Number> T min(String field) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty for min operation.");
        }
        String originalSelect = this.selectString;
        try {
            this.selectString = "MIN(" + validateIdentifier(field) + ")";
            Number result = jdbcTemplate.queryForObject(buildSelectSql(), buildWhereValues().toArray(), Number.class);
            return result != null ? (T) result : null;
        } finally {
            this.selectString = originalSelect;
        }
    }

    public <K, T extends Number> T min(StringFunction<K> field) {
        String fieldName = FieldNameUtils.getSimpleFieldName(field);
        return min(fieldName);
    }

    public <T extends Number> T avg(String field) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty for avg operation.");
        }
        String originalSelect = this.selectString;
        try {
            this.selectString = "AVG(" + validateIdentifier(field) + ")";
            Number result = jdbcTemplate.queryForObject(buildSelectSql(), buildWhereValues().toArray(), Number.class);
            return result != null ? (T) result : null;
        } finally {
            this.selectString = originalSelect;
        }
    }

    public <K, T extends Number> T avg(StringFunction<K> field) {
        String fieldName = FieldNameUtils.getSimpleFieldName(field);
        return avg(fieldName);
    }

    public <T extends Number> T sum(String field) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty for sum operation.");
        }
        String originalSelect = this.selectString;
        try {
            this.selectString = "SUM(" + validateIdentifier(field) + ")";
            Number result = jdbcTemplate.queryForObject(buildSelectSql(), buildWhereValues().toArray(), Number.class);
            return result != null ? (T) result : null;
        } finally {
            this.selectString = originalSelect;
        }
    }

    public <K, T extends Number> T sum(StringFunction<K> field) {
        String fieldName = FieldNameUtils.getSimpleFieldName(field);
        return sum(fieldName);
    }

    // ==================== 增删改 ====================

    public int insert(Object obj) {
        if (obj == null) {
            return 0;
        }
        HashMap<String, Object> objectHashMap = getObjectHashMap(obj, autoIncrement);
        return add(objectHashMap);
    }

    public int insert(HashMap<String, Object> values) {
        return add(values);
    }

    public int add(HashMap<String, Object> values) {
        if (autoFill) {
            values.put("`" + insertTimeField + "`", getTimestamp());
            values.put("`" + updateTimeField + "`", getTimestamp());
        }
        StringJoiner insertField = new StringJoiner(",");
        StringJoiner insertPlaceholder = new StringJoiner(",");
        List<Object> paramValues = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = validateIdentifier(entry.getKey());
            insertField.add(key);
            insertPlaceholder.add("?");
            paramValues.add(entry.getValue());
        }
        String sql = buildInsertSql(insertField.toString(), insertPlaceholder.toString());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < paramValues.size(); i++) {
                ps.setObject(i + 1, paramValues.get(i));
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().intValue() : 0;
    }

    public int add(Object obj) {
        if (obj == null) {
            return 0;
        }
        HashMap<String, Object> objectHashMap = getObjectHashMap(obj, autoIncrement);
        return add(objectHashMap);
    }

    public int delete() {
        if (tableName == null) {
            throw new IllegalArgumentException("Table name cannot be null.");
        }
        if (buildWhereValues().isEmpty()) {
            throw new IllegalArgumentException("Where clause cannot be empty.");
        }
        if (softDelete) {
            HashMap<String, Object> deleteValues = new HashMap<>();
            deleteValues.put("deleted_at", getTimestamp());
            return update(deleteValues);
        }
        String sql = "delete from " + tableName + buildWhereString();
        return jdbcTemplate.update(sql, buildWhereValues().toArray());
    }

    public int update(HashMap<String, Object> values) {
        check();
        if (autoFill) {
            values.put("`" + updateTimeField + "`", getTimestamp());
        }
        StringJoiner updateField = new StringJoiner(",");
        List<Object> paramValues = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = validateIdentifier(entry.getKey());
            updateField.add(key + "=?");
            paramValues.add(entry.getValue());
        }
        paramValues.addAll(buildWhereValues());
        String sql = buildUpdateSql(updateField.toString());
        return jdbcTemplate.update(sql, paramValues.toArray());
    }

    public int update(Object obj) {
        if (obj == null) {
            return 0;
        }
        HashMap<String, Object> objectHashMap = getObjectHashMap(obj, autoIncrement);
        return update(objectHashMap);
    }

    public <K> int increment(StringFunction<K> key) {
        String fieldName = FieldNameUtils.getSimpleFieldName(key);
        check();
        String sql = "update " + getTableName() + " set " + fieldName + "=" + fieldName + "+1" + buildWhereString();
        return jdbcTemplate.update(sql, buildWhereValues().toArray());
    }

    public int increment(String fieldName) {
        check();
        String sql = "update " + getTableName() + " set " + fieldName + "=" + fieldName + "+1" + buildWhereString();
        return jdbcTemplate.update(sql, buildWhereValues().toArray());
    }

    public <K> int decrement(StringFunction<K> key) {
        String fieldName = FieldNameUtils.getSimpleFieldName(key);
        check();
        String sql = "update " + getTableName() + " set " + fieldName + "=" + fieldName + "-1" + buildWhereString();
        return jdbcTemplate.update(sql, buildWhereValues().toArray());
    }

    public int decrement(String fieldName) {
        check();
        String sql = "update " + getTableName() + " set " + fieldName + "=" + fieldName + "-1" + buildWhereString();
        return jdbcTemplate.update(sql, buildWhereValues().toArray());
    }

    // ==================== 内部方法 ====================

    private void check() {
        if (tableName == null) {
            throw new IllegalArgumentException("Table name cannot be null.");
        }
        if (buildWhereValues().isEmpty()) {
            throw new IllegalArgumentException("Where clause cannot be empty.");
        }
    }

    private String getTableName() {
        if (tableName == null) {
            throw new IllegalArgumentException("Table name cannot be null.");
        }
        return validateIdentifier(tableName);
    }

    private String buildUpdateSql(String fields) {
        return "update " + getTableName() + " set " + fields + buildWhereString();
    }

    private String buildInsertSql(String fields, String placeholders) {
        return "insert into " + getTableName() + "(" + fields + ") values(" + placeholders + ")";
    }
}

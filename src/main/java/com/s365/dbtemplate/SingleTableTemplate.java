package com.s365.dbtemplate;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 单表泛型查询模板
 * 提供类型安全的链式查询 API，内部委托给 QueryTemplate
 */
public class SingleTableTemplate<T> extends BaseDbTemplate {

    private final QueryTemplate<T> queryTemplate;
    private final Class<T> entityClass;

    public SingleTableTemplate(JdbcTemplate jdbcTemplate, Class<T> entityClass) {
        super(jdbcTemplate);
        this.entityClass = entityClass;
        this.queryTemplate = new QueryTemplate<T>(jdbcTemplate).table(entityClass);
    }

    public SingleTableTemplate(JdbcTemplate jdbcTemplate, String tableName) {
        super(jdbcTemplate);
        this.entityClass = null;
        this.queryTemplate = new QueryTemplate<T>(jdbcTemplate).table(tableName);
    }

    // 配置方法
    public SingleTableTemplate<T> unUseAutoFill() {
        queryTemplate.unUseAutoFill();
        return this;
    }

    public SingleTableTemplate<T> setUpdateTimeField(String updateTimeField) {
        queryTemplate.setUpdateTimeField(updateTimeField);
        return this;
    }

    public SingleTableTemplate<T> setInsertTimeField(String insertTimeField) {
        queryTemplate.setInsertTimeField(insertTimeField);
        return this;
    }

    public SingleTableTemplate<T> lockForUpdate() {
        queryTemplate.lockForUpdate();
        return this;
    }

    public SingleTableTemplate<T> lockForShare() {
        queryTemplate.lockForShare();
        return this;
    }

    public SingleTableTemplate<T> unUseSoftDelete() {
        queryTemplate.unUseSoftDelete();
        return this;
    }

    public SingleTableTemplate<T> setSoftDeleteValue(Object softDeleteValue) {
        queryTemplate.setSoftDeleteValue(softDeleteValue);
        return this;
    }

    // WHERE 方法
    public <V> SingleTableTemplate<T> where(String key, V value) {
        queryTemplate.where(key, value);
        return this;
    }

    public <K, V> SingleTableTemplate<T> where(StringFunction<T> field, V value) {
        queryTemplate.where(field, value);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereNe(StringFunction<T> field, V value) {
        queryTemplate.whereNe(field, value);
        return this;
    }

    public <V> SingleTableTemplate<T> whereNe(String key, V value) {
        queryTemplate.whereNe(key, value);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereLt(StringFunction<T> field, V value) {
        queryTemplate.whereLt(field, value);
        return this;
    }

    public <V> SingleTableTemplate<T> whereLt(String key, V value) {
        queryTemplate.whereLt(key, value);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereLe(StringFunction<T> field, V value) {
        queryTemplate.whereLe(field, value);
        return this;
    }

    public <V> SingleTableTemplate<T> whereLe(String key, V value) {
        queryTemplate.whereLe(key, value);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereGt(StringFunction<T> field, V value) {
        queryTemplate.whereGt(field, value);
        return this;
    }

    public <V> SingleTableTemplate<T> whereGt(String key, V value) {
        queryTemplate.whereGt(key, value);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereGe(StringFunction<T> field, V value) {
        queryTemplate.whereGe(field, value);
        return this;
    }

    public <V> SingleTableTemplate<T> whereGe(String key, V value) {
        queryTemplate.whereGe(key, value);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereLike(StringFunction<T> field, V value) {
        queryTemplate.whereLike(field, value);
        return this;
    }

    public <V> SingleTableTemplate<T> whereLike(String key, V value) {
        queryTemplate.whereLike(key, value);
        return this;
    }

    public <V> SingleTableTemplate<T> whereIsNull(StringFunction<T> field) {
        queryTemplate.whereIsNull(field);
        return this;
    }

    public SingleTableTemplate<T> whereIsNull(String key) {
        queryTemplate.whereIsNull(key);
        return this;
    }

    public SingleTableTemplate<T> whereNotNull(String key) {
        queryTemplate.whereNotNull(key);
        return this;
    }

    public <V> SingleTableTemplate<T> whereNotNull(StringFunction<T> field) {
        queryTemplate.whereNotNull(field);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereFindInSet(StringFunction<T> field, V value) {
        queryTemplate.whereFindInSet(field, value);
        return this;
    }

    public <V> SingleTableTemplate<T> whereFindInSet(String key, V value) {
        queryTemplate.whereFindInSet(key, value);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereIn(StringFunction<T> field, List<V> values) {
        queryTemplate.whereIn(field, values);
        return this;
    }

    public <V> SingleTableTemplate<T> whereIn(String key, List<V> values) {
        queryTemplate.whereIn(key, values);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereNotIn(StringFunction<T> field, List<V> values) {
        queryTemplate.whereNotIn(field, values);
        return this;
    }

    public <V> SingleTableTemplate<T> whereNotIn(String key, List<V> values) {
        queryTemplate.whereNotIn(key, values);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereOr(StringFunction<T> field, V value) {
        queryTemplate.whereOr(field, value);
        return this;
    }

    public <V> SingleTableTemplate<T> whereOr(String key, V value) {
        queryTemplate.whereOr(key, value);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereOrNe(StringFunction<T> field, V value) {
        queryTemplate.whereOrNe(field, value);
        return this;
    }

    public <V> SingleTableTemplate<T> whereOrNe(String key, V value) {
        queryTemplate.whereOrNe(key, value);
        return this;
    }

    public <K, V> SingleTableTemplate<T> whereOrLike(StringFunction<T> field, V value) {
        queryTemplate.whereOrLike(field, value);
        return this;
    }

    public <V> SingleTableTemplate<T> whereOrLike(String key, V value) {
        queryTemplate.whereOrLike(key, value);
        return this;
    }

    // SELECT / JOIN / ORDER / GROUP / LIMIT
    public SingleTableTemplate<T> select(String field) {
        queryTemplate.select(field);
        return this;
    }

    public <K> SingleTableTemplate<T> select(StringFunction<T> key) {
        queryTemplate.select(key);
        return this;
    }



    public SingleTableTemplate<T> leftJoin(String TABLE_NAME, String localField, String foreignField) {
        queryTemplate.leftJoin(TABLE_NAME, localField, foreignField);
        return this;
    }

    public SingleTableTemplate<T> rightJoin(String TABLE_NAME, String localField, String foreignField) {
        queryTemplate.rightJoin(TABLE_NAME, localField, foreignField);
        return this;
    }



    public SingleTableTemplate<T> innerJoin(String TABLE_NAME, String localField, String foreignField) {
        queryTemplate.innerJoin(TABLE_NAME, localField, foreignField);
        return this;
    }

    public SingleTableTemplate<T> orderByDesc(String field) {
        queryTemplate.orderByDesc(field);
        return this;
    }

    public <K> SingleTableTemplate<T> orderByDesc(StringFunction<T> field) {
        queryTemplate.orderByDesc(field);
        return this;
    }

    public SingleTableTemplate<T> orderByAsc(String field) {
        queryTemplate.orderByAsc(field);
        return this;
    }

    public <K> SingleTableTemplate<T> orderByAsc(StringFunction<T> field) {
        queryTemplate.orderByAsc(field);
        return this;
    }

    public SingleTableTemplate<T> groupBy(String field) {
        queryTemplate.groupBy(field);
        return this;
    }

    public <K> SingleTableTemplate<T> groupBy(StringFunction<T> field) {
        queryTemplate.groupBy(field);
        return this;
    }

    public SingleTableTemplate<T> limit(int size) {
        queryTemplate.limit(size);
        return this;
    }

    public SingleTableTemplate<T> limit(int size, int offset) {
        queryTemplate.limit(size, offset);
        return this;
    }

    // 查询执行
    public Boolean exists() {
        return queryTemplate.exists();
    }

    public T first() {
        return queryTemplate.first(entityClass);
    }

    public T one() {
        return queryTemplate.one(entityClass);
    }

    public List<T> get() {
        return queryTemplate.get(entityClass);
    }

    public List<T> list() {
        return queryTemplate.list(entityClass);
    }

    public PageResult<T> page(int page, int size) {
        return queryTemplate.page(entityClass, page, size);
    }

    // Map 查询
    public java.util.Map<String, Object> firstMap() {
        return queryTemplate.firstMap();
    }

    public java.util.List<java.util.Map<String, Object>> getMaps() {
        return queryTemplate.getMaps();
    }

    public java.util.List<java.util.Map<String, Object>> listMaps() {
        return queryTemplate.listMaps();
    }

    public PageResult<java.util.Map<String, Object>> pageMaps(int page, int size) {
        return queryTemplate.pageMaps(page, size);
    }

    // 聚合
    public Long count() {
        return queryTemplate.count();
    }

    public <K> Long count(StringFunction<T> field) {
        return queryTemplate.count(field);
    }

    public <R extends Number> R max(String field) {
        return queryTemplate.max(field);
    }

    public <K, R extends Number> R max(StringFunction<T> field) {
        return queryTemplate.max(field);
    }

    public <R extends Number> R min(String field) {
        return queryTemplate.min(field);
    }

    public <K, R extends Number> R min(StringFunction<T> field) {
        return queryTemplate.min(field);
    }

    public <R extends Number> R avg(String field) {
        return queryTemplate.avg(field);
    }

    public <K, R extends Number> R avg(StringFunction<T> field) {
        return queryTemplate.avg(field);
    }

    public <R extends Number> R sum(String field) {
        return queryTemplate.sum(field);
    }

    public <K, R extends Number> R sum(StringFunction<T> field) {
        return queryTemplate.sum(field);
    }

    // 增删改
    public int insert(Object obj) {
        return queryTemplate.insert(obj);
    }

    public int insert(java.util.HashMap<String, Object> values) {
        return queryTemplate.insert(values);
    }

    public int add(Object obj) {
        return queryTemplate.add(obj);
    }

    public int delete() {
        return queryTemplate.delete();
    }

    public int update(Object obj) {
        return queryTemplate.update(obj);
    }

    public int update(java.util.HashMap<String, Object> values) {
        return queryTemplate.update(values);
    }

    public <K> int increment(StringFunction<T> key) {
        return queryTemplate.increment(key);
    }

    public int increment(String fieldName) {
        return queryTemplate.increment(fieldName);
    }

    public <K> int decrement(StringFunction<T> key) {
        return queryTemplate.decrement(key);
    }

    public int decrement(String fieldName) {
        return queryTemplate.decrement(fieldName);
    }
}

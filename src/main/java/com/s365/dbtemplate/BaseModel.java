package com.s365.dbtemplate;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 基础模型类，提供通用的数据库单表操作（增删改查）
 * @param <T> 实体类类型
 */
public class BaseModel<T> {

    public Integer insert(T entity) {
        return getTable().insert(entity);
    }

    public Integer update(T entity) {
        Object idValue = getFieldValue(entity, "id");
        return getTable().where("id", idValue).update(entity);
    }

    public T detail(Integer id) {
        return getTable().where("id", id).first();
    }

    public Integer delete(Integer id) {
        return getTable().where("id", id).delete();
    }

    public List<T> all() {
        return getTable().list();
    }

    public List<T> all(Boolean useSoftDelete) {
        return getTable(useSoftDelete).list();
    }

    public Integer trueDelete(Integer id) {
        return getTable(false).where("id", id).delete();
    }

    @SuppressWarnings("unchecked")
    protected SingleTableTemplate<T> getTable() {
        Class<T> clazz = getTClass();
        return Db.singleTable(clazz);
    }

    @SuppressWarnings("unchecked")
    protected SingleTableTemplate<T> getTable(Boolean useSoftDelete) {
        Class<T> clazz = getTClass();
        return Db.singleTable(clazz, !useSoftDelete);
    }

    private Class<T> getTClass() {
        return (Class<T>) ((java.lang.reflect.ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    protected Object getFieldValue(T entity, String fieldName) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(entity);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("获取字段" + fieldName + "失败", e);
        }
    }
}

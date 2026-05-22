package com.s365.dbtemplate;

import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.UUID;

/**
 * 数据库模板基类
 * 提供通用的数据库操作方法和工具方法
 */
public abstract class BaseDbTemplate {
    protected final JdbcTemplate jdbcTemplate;

    public BaseDbTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 生成 UUID 前 8 位
     */
    protected static String getUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取当前时间戳（秒）
     */
    protected long getTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 验证 SQL 标识符（表名、字段名等），防止 SQL 注入
     */
    protected String validateIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        // 只允许字母、数字、下划线、点和反引号
        if (!identifier.matches("^[a-zA-Z0-9_.`]+$")) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
        }
        return identifier;
    }

    /**
     * 创建 RowMapper 用于对象映射
     */
    protected <R> RowMapper<R> getRowMapper(Class<R> entityClass) {
        if (BeanUtils.isSimpleProperty(entityClass)) {
            return new SingleColumnRowMapper<>(entityClass);
        }
        return new BeanPropertyRowMapper<>(entityClass);
    }

    /**
     * 将对象转换为 HashMap（用于 insert）
     */
    protected HashMap<String, Object> getObjectHashMap(Object obj, Boolean autoIncrement) {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        HashMap<String, Object> result = new HashMap<>();
        for (Field declaredField : fields) {
            declaredField.setAccessible(true);
            String name = declaredField.getName();
            name = ClassNameUtils.camelToUnderscore(name);
            Object o = null;
            try {
                o = declaredField.get(obj);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (o == null) {
                continue;
            }
            if (autoIncrement && "id".equals(name)) {
                continue;
            }
            result.put("`" + name + "`", o);
        }
        return result;
    }
}

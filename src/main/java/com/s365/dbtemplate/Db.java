package com.s365.dbtemplate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DB Template 核心入口类
 * 提供静态方法用于数据库操作
 */
@Slf4j
public class Db {
    private static JdbcTemplate connection;

    /**
     * 初始化 JdbcTemplate
     * @param jdbcTemplate Spring JdbcTemplate 实例
     */
    public static void init(JdbcTemplate jdbcTemplate) {
        connection = jdbcTemplate;
        log.info("Db initialized with JdbcTemplate: {}", connection);
    }

    /**
     * 获取当前的 JdbcTemplate 连接
     * @return JdbcTemplate 实例
     */
    public static JdbcTemplate getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Db has not been initialized. Please make sure Config is properly configured.");
        }
        return connection;
    }

    // ==================== TableTemplate 入口 ====================

    public static <T> QueryTemplate<T> table(Class<T> clazz) {
        return new TableTemplate(getConnection()).table(clazz);
    }

    public static QueryTemplate<?> table(String tableName) {
        return new TableTemplate(getConnection()).table(tableName);
    }

    public static <T> QueryTemplate<T> table(Class<T> clazz, Boolean unUseSoftDelete) {
        QueryTemplate<T> query = new TableTemplate(getConnection()).table(clazz);
        if (Boolean.TRUE.equals(unUseSoftDelete)) {
            query.unUseSoftDelete();
        }
        return query;
    }

    public static <T> QueryTemplate<T> table(Class<T> clazz, Boolean unUseSoftDelete, Boolean unUseAutoFill) {
        QueryTemplate<T> query = new TableTemplate(getConnection()).table(clazz);
        if (Boolean.TRUE.equals(unUseSoftDelete)) {
            query.unUseSoftDelete();
        }
        if (Boolean.TRUE.equals(unUseAutoFill)) {
            query.unUseAutoFill();
        }
        return query;
    }

    // ==================== SingleTableTemplate 入口 ====================

    public static <T> SingleTableTemplate<T> singleTable(Class<T> clazz) {
        return new SingleTableTemplate<>(getConnection(), clazz);
    }

    public static SingleTableTemplate<Object> singleTable(String tableName) {
        return new SingleTableTemplate<>(getConnection(), tableName);
    }

    public static <T> SingleTableTemplate<T> singleTable(Class<T> clazz, Boolean unUseSoftDelete) {
        SingleTableTemplate<T> template = new SingleTableTemplate<>(getConnection(), clazz);
        if (Boolean.TRUE.equals(unUseSoftDelete)) {
            template.unUseSoftDelete();
        }
        return template;
    }

    public static <T> SingleTableTemplate<T> singleTable(Class<T> clazz, Boolean unUseSoftDelete, Boolean unUseAutoFill) {
        SingleTableTemplate<T> template = new SingleTableTemplate<>(getConnection(), clazz);
        if (Boolean.TRUE.equals(unUseSoftDelete)) {
            template.unUseSoftDelete();
        }
        if (Boolean.TRUE.equals(unUseAutoFill)) {
            template.unUseAutoFill();
        }
        return template;
    }

    /**
     * 获取 JdbcTemplate 连接（兼容旧版本）
     * @return JdbcTemplate 实例
     */
    public static JdbcTemplate connection() {
        return getConnection();
    }
}

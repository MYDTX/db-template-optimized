package com.s365.dbtemplate;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 多表查询模板入口类
 * 通过 Db.table() 获取 QueryTemplate 实例进行链式查询
 */
public class TableTemplate extends BaseDbTemplate {

    public TableTemplate(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /**
     * 指定表名开始查询
     */
    public QueryTemplate<?> table(String tableName) {
        return new QueryTemplate<>(jdbcTemplate).table(tableName);
    }

    /**
     * 指定实体类开始查询
     */
    public <T> QueryTemplate<T> table(Class<T> entityClass) {
        return new QueryTemplate<T>(jdbcTemplate).table(entityClass);
    }
}

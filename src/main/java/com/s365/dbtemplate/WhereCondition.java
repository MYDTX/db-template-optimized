package com.s365.dbtemplate;

/**
 * WHERE 条件封装
 */
public class WhereCondition {
    public final String field;
    public final String operator;
    public final Object value;
    public final String uuid;
    public final boolean likeOperation;
    public final boolean orCondition;

    public WhereCondition(String field, String operator, Object value, String uuid) {
        this(field, operator, value, uuid, false, false);
    }

    public WhereCondition(String field, String operator, Object value, String uuid, boolean likeOperation) {
        this(field, operator, value, uuid, likeOperation, false);
    }

    public WhereCondition(String field, String operator, Object value, String uuid, boolean likeOperation, boolean orCondition) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.uuid = uuid;
        this.likeOperation = likeOperation;
        this.orCondition = orCondition;
    }
}

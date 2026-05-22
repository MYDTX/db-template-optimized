package com.s365.dbtemplate;

import java.util.List;

/**
 * WHERE IN 条件封装
 */
public class WhereInCondition {
    public final String field;
    public final String operator;
    public final List<?> values;
    public final String uuid;

    public WhereInCondition(String field, String operator, List<?> values, String uuid) {
        this.field = field;
        this.operator = operator;
        this.values = values;
        this.uuid = uuid;
    }
}

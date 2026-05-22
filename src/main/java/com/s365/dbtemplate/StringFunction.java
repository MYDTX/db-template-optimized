package com.s365.dbtemplate;

import java.io.Serializable;

/**
 * 支持通过 SerializedLambda 解析字段名的函数式接口
 */
@FunctionalInterface
public interface StringFunction<T> extends Serializable {
    Object apply(T t);
}

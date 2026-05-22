package com.s365.dbtemplate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.beans.Introspector;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通过 Lambda 表达式解析字段名
 * 优化：缓存使用 SerializedLambda 签名作为 key，避免 lambda 实例不同导致缓存失效
 */
@Slf4j
public class FieldNameUtils {

    /**
     * 缓存 key 使用 SerializedLambda 的 implClass + implMethodName 组合，
     * 而不是 lambda 实例本身，避免每次 new lambda 导致缓存失效
     */
    private static final Map<String, Field> FUNCTION_CACHE = new ConcurrentHashMap<>();

    private FieldNameUtils() {}

    public static <T> String getFieldName(StringFunction<T> function) {
        Field field = getField(function);
        return field.getName();
    }

    public static <T> String getSimpleFieldName(StringFunction<T> function) {
        Field field = getField(function);
        String className = field.getDeclaringClass().getSimpleName();
        className = ClassNameUtils.camelToUnderscore(className);
        String fieldName = ClassNameUtils.camelToUnderscore(field.getName());
        return className + "." + fieldName;
    }

    public static <T> Field getField(StringFunction<T> function) {
        // 使用 SerializedLambda 签名作为缓存 key
        String cacheKey = getCacheKey(function);
        return FUNCTION_CACHE.computeIfAbsent(cacheKey, k -> findField(function));
    }

    private static <T> String getCacheKey(StringFunction<T> function) {
        try {
            SerializedLambda sl = getSerializedLambda(function);
            return sl.getImplClass() + "#" + sl.getImplMethodName();
        } catch (Exception e) {
            // 如果无法获取 SerializedLambda，回退到对象 hashCode
            return String.valueOf(function.hashCode());
        }
    }

    private static <T> Field findField(StringFunction<T> function) {
        final SerializedLambda serializedLambda = getSerializedLambda(function);
        final String implMethodName = serializedLambda.getImplMethodName();
        final String fieldName = convertToFieldName(implMethodName);
        final Field field = resolveField(fieldName, serializedLambda);

        if (field == null) {
            throw new RuntimeException("No such class 「" + serializedLambda.getImplClass() + "」 field 「" + fieldName + "」.");
        }
        return field;
    }

    private static Field resolveField(String fieldName, SerializedLambda serializedLambda) {
        try {
            String declaredClass = serializedLambda.getImplClass().replace("/", ".");
            Class<?> aClass = loadClass(declaredClass);
            if (aClass != null) {
                return ReflectionUtils.findField(aClass, fieldName);
            }
            throw new ClassNotFoundException(declaredClass);
        } catch (ClassNotFoundException e) {
            log.error("Class not found: {} with field: {}", serializedLambda.getImplClass(), fieldName, e);
            throw new IllegalArgumentException("get class field exception. Class: " + serializedLambda.getImplClass() + ", Field: " + fieldName, e);
        }
    }

    private static Class<?> loadClass(String declaredClass) {
        // 1. 当前线程上下文类加载器
        try {
            return Class.forName(declaredClass, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException ignored) {}

        // 2. Spring 默认类加载器
        try {
            return Class.forName(declaredClass, false, ClassUtils.getDefaultClassLoader());
        } catch (ClassNotFoundException ignored) {}

        // 3. 系统类加载器
        try {
            return Class.forName(declaredClass, false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException ignored) {}

        return null;
    }

    private static String convertToFieldName(String getterMethodName) {
        String prefix = null;
        if (getterMethodName.startsWith("get")) {
            prefix = "get";
        } else if (getterMethodName.startsWith("is")) {
            prefix = "is";
        }

        if (prefix == null) {
            throw new IllegalArgumentException("invalid getter method: " + getterMethodName);
        }

        String fieldName = getterMethodName.substring(prefix.length());
        return Introspector.decapitalize(fieldName);
    }

    private static <T> SerializedLambda getSerializedLambda(StringFunction<T> function) {
        try {
            Method method = function.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            return (SerializedLambda) method.invoke(function);
        } catch (Exception e) {
            throw new RuntimeException("get SerializedLambda exception.", e);
        }
    }
}

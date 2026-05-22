package com.s365.dbtemplate;

/**
 * 类名转换工具类
 */
public class ClassNameUtils {

    private ClassNameUtils() {}

    /**
     * 获取类的简单名称（不带包名）
     */
    public static String getSimpleClassName(Object obj) {
        return obj.getClass().getSimpleName();
    }

    /**
     * 获取类的全限定名称（带包名）
     */
    public static String getFullClassName(Object obj) {
        return obj.getClass().getName();
    }

    /**
     * 驼峰命名转下划线命名（如：UserInfo -> user_info）
     */
    public static String camelToUnderscore(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 获取类名并转换为下划线格式
     */
    public static <R> String getClassNameAsUnderscore(Class<R> clazz) {
        return camelToUnderscore(clazz.getSimpleName());
    }

    /**
     * 下划线命名转驼峰命名（如：user_info -> userInfo）
     */
    public static String underscoreToCamel(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        return result.toString();
    }
}

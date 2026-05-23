package com.s365.dbtemplate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassNameUtilsTest {

    @Test
    void testCamelToUnderscore() {
        assertEquals("user_info", ClassNameUtils.camelToUnderscore("UserInfo"));
        assertEquals("user", ClassNameUtils.camelToUnderscore("User"));
        assertEquals("my_awesome_class", ClassNameUtils.camelToUnderscore("MyAwesomeClass"));
        // 连续大写字母（如缩写）每个大写字母单独加下划线
        assertEquals("h_t_m_l_parser", ClassNameUtils.camelToUnderscore("HTMLParser"));
    }

    @Test
    void testCamelToUnderscoreLowerFirst() {
        assertEquals("user_info", ClassNameUtils.camelToUnderscore("userInfo"));
        assertEquals("my_class", ClassNameUtils.camelToUnderscore("myClass"));
    }

    @Test
    void testCamelToUnderscoreNullAndEmpty() {
        assertNull(ClassNameUtils.camelToUnderscore(null));
        assertEquals("", ClassNameUtils.camelToUnderscore(""));
    }

    @Test
    void testUnderscoreToCamel() {
        assertEquals("userInfo", ClassNameUtils.underscoreToCamel("user_info"));
        assertEquals("user", ClassNameUtils.underscoreToCamel("user"));
        assertEquals("myAwesomeClass", ClassNameUtils.underscoreToCamel("my_awesome_class"));
    }

    @Test
    void testUnderscoreToCamelNullAndEmpty() {
        assertNull(ClassNameUtils.underscoreToCamel(null));
        assertEquals("", ClassNameUtils.underscoreToCamel(""));
    }

    @Test
    void testGetClassNameAsUnderscore() {
        assertEquals("class_name_utils", ClassNameUtils.getClassNameAsUnderscore(ClassNameUtils.class));
        assertEquals("string", ClassNameUtils.getClassNameAsUnderscore(String.class));
    }

    @Test
    void testGetFullClassName() {
        String fullName = ClassNameUtils.getFullClassName("test");
        assertEquals("java.lang.String", fullName);
    }
}

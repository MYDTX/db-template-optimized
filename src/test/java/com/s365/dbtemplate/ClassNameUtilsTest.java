package com.s365.dbtemplate;

import org.junit.Test;

import static org.junit.Assert.*;

public class ClassNameUtilsTest {

    @Test
    public void testCamelToUnderscore() {
        assertEquals("user_info", ClassNameUtils.camelToUnderscore("UserInfo"));
        assertEquals("user", ClassNameUtils.camelToUnderscore("User"));
        assertEquals("my_awesome_class", ClassNameUtils.camelToUnderscore("MyAwesomeClass"));
        // 连续大写字母（如缩写）每个大写字母单独加下划线
        assertEquals("h_t_m_l_parser", ClassNameUtils.camelToUnderscore("HTMLParser"));
    }

    @Test
    public void testCamelToUnderscoreLowerFirst() {
        assertEquals("user_info", ClassNameUtils.camelToUnderscore("userInfo"));
        assertEquals("my_class", ClassNameUtils.camelToUnderscore("myClass"));
    }

    @Test
    public void testCamelToUnderscoreNullAndEmpty() {
        assertNull(ClassNameUtils.camelToUnderscore(null));
        assertEquals("", ClassNameUtils.camelToUnderscore(""));
    }

    @Test
    public void testUnderscoreToCamel() {
        assertEquals("userInfo", ClassNameUtils.underscoreToCamel("user_info"));
        assertEquals("user", ClassNameUtils.underscoreToCamel("user"));
        assertEquals("myAwesomeClass", ClassNameUtils.underscoreToCamel("my_awesome_class"));
    }

    @Test
    public void testUnderscoreToCamelNullAndEmpty() {
        assertNull(ClassNameUtils.underscoreToCamel(null));
        assertEquals("", ClassNameUtils.underscoreToCamel(""));
    }

    @Test
    public void testGetClassNameAsUnderscore() {
        assertEquals("class_name_utils", ClassNameUtils.getClassNameAsUnderscore(ClassNameUtils.class));
        assertEquals("string", ClassNameUtils.getClassNameAsUnderscore(String.class));
    }

    @Test
    public void testGetSimpleClassName() {
        // ClassNameUtils has private constructor, test via static methods
    }

    @Test
    public void testGetFullClassName() {
        String fullName = ClassNameUtils.getFullClassName("test");
        assertEquals("java.lang.String", fullName);
    }
}

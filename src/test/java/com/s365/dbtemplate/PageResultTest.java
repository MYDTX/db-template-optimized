package com.s365.dbtemplate;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void testPageResultBasic() {
        List<String> data = Arrays.asList("a", "b", "c");
        PageResult<String> result = new PageResult<>(25L, 1, 10, data);

        assertEquals(25L, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getPageSize());
        assertEquals(data, result.getData());
        assertEquals(3, result.getLastPage());
    }

    @Test
    void testPageResultLastPageExact() {
        List<String> data = Collections.singletonList("a");
        PageResult<String> result = new PageResult<>(20L, 1, 10, data);

        assertEquals(2, result.getLastPage());
    }

    @Test
    void testPageResultLastPageSingle() {
        List<String> data = Collections.singletonList("a");
        PageResult<String> result = new PageResult<>(5L, 1, 10, data);

        assertEquals(1, result.getLastPage());
    }

    @Test
    void testPageResultZeroTotal() {
        PageResult<String> result = new PageResult<>(0L, 1, 10, Collections.emptyList());

        assertEquals(0, result.getLastPage());
    }

    @Test
    void testPageResultZeroSize() {
        PageResult<String> result = new PageResult<>(10L, 1, 0, Collections.emptyList());

        assertEquals(0, result.getLastPage());
    }
}

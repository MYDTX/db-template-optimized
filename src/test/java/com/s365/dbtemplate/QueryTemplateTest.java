package com.s365.dbtemplate;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 测试 QueryTemplate 核心功能：
 * - 聚合方法不修改 selectString
 * - 软删除值可配置
 * - 空结果安全返回 null
 * - lock 默认不拼接
 */
public class QueryTemplateTest {

    private JdbcTemplate jdbcTemplate;
    private QueryTemplate<Object> query;

    @Before
    public void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        query = new QueryTemplate<>(jdbcTemplate);
        query.table("test_table");
    }

    // ==================== 1. 聚合方法不修改 selectString ====================

    @Test
    @SuppressWarnings("unchecked")
    public void countShouldGenerateCountSql() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(5L);

        query.where("status", 1).count();

        // 验证 count 产生的 SQL 包含 COUNT(*)
        verify(jdbcTemplate).queryForObject(
                argThat(sql -> ((String) sql).contains("COUNT(*)")),
                any(Object[].class),
                eq(Number.class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void countShouldNotAffectSubsequentSelectQueries() {
        // count 返回 5
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(5L);

        query.where("status", 1);
        query.count(); // 调用 count

        // 调用 count 后，后续操作的 SQL 应该仍然是标准 SELECT
        String[] capturedSql = new String[1];
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(invocation -> {
                    capturedSql[0] = invocation.getArgument(0);
                    return Collections.emptyList();
                });

        query.first(Object.class);

        assertNotNull(capturedSql[0]);
        assertTrue("SQL should start with SELECT", capturedSql[0].toUpperCase().startsWith("SELECT"));
        // 不应该包含 COUNT
        assertFalse("SQL should not contain COUNT after count() call", capturedSql[0].toUpperCase().contains("COUNT"));
        // 应该包含原始 SELECT 字段（默认 tableName.*）
        assertTrue("SQL should contain test_table.*", capturedSql[0].contains("test_table.*"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void countWithFieldShouldUseCountField() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(3L);

        query.count("status");

        verify(jdbcTemplate).queryForObject(
                argThat(sql -> ((String) sql).contains("COUNT(status)")),
                any(Object[].class),
                eq(Number.class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void maxShouldNotAffectSubsequentQueries() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(100);

        query.select("id");
        query.max("price");

        // 后续查询应该使用原始 select
        String[] capturedSql = new String[1];
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(invocation -> {
                    capturedSql[0] = invocation.getArgument(0);
                    return Collections.emptyList();
                });

        query.first(Object.class);

        assertNotNull(capturedSql[0]);
        assertTrue("SQL should contain original SELECT id", capturedSql[0].contains("id"));
        assertFalse("SQL should not contain MAX", capturedSql[0].toUpperCase().contains("MAX"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void minShouldNotAffectSubsequentQueries() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(10);

        query.select("name");
        query.min("price");

        String[] capturedSql = new String[1];
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(invocation -> {
                    capturedSql[0] = invocation.getArgument(0);
                    return Collections.emptyList();
                });

        query.first(Object.class);

        assertNotNull(capturedSql[0]);
        assertTrue("SQL should contain original SELECT name", capturedSql[0].contains("name"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void avgShouldGenerateAvgSql() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(55.5);

        query.avg("score");

        verify(jdbcTemplate).queryForObject(
                argThat(sql -> ((String) sql).contains("AVG(score)")),
                any(Object[].class),
                eq(Number.class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sumShouldGenerateSumSql() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(500);

        query.sum("amount");

        verify(jdbcTemplate).queryForObject(
                argThat(sql -> ((String) sql).contains("SUM(amount)")),
                any(Object[].class),
                eq(Number.class)
        );
    }

    // ==================== 2. 空结果安全返回 null ====================

    @Test
    public void maxShouldReturnNullWhenNoResult() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(null);

        Number result = query.max("price");

        assertNull(result);
    }

    @Test
    public void minShouldReturnNullWhenNoResult() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(null);

        Number result = query.min("price");

        assertNull(result);
    }

    // ==================== 3. 软删除值可配置 ====================

    @Test
    @SuppressWarnings("unchecked")
    public void softDeleteCustomValueShouldBePassedAsParam() {
        query.setSoftDeleteValue(-1L);
        query.where("status", 1);

        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(1L);

        query.count();

        // 验证参数中包含 -1
        verify(jdbcTemplate).queryForObject(anyString(), argThat((Object[] args) -> {
            for (Object arg : args) {
                if (arg instanceof Long && ((Long) arg) == -1L) return true;
            }
            return false;
        }), eq(Number.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void softDeleteDefaultValueShouldBeZeroInParams() {
        query.where("status", 1);

        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(1L);

        query.count();

        verify(jdbcTemplate).queryForObject(anyString(), argThat((Object[] args) -> {
            for (Object arg : args) {
                if (arg instanceof Integer && ((Integer) arg) == 0) return true;
            }
            return false;
        }), eq(Number.class));
    }

    @Test
    public void unUseSoftDeleteShouldRemoveDeletedAtFromSql() {
        query.unUseSoftDelete();
        query.where("status", 1);

        String sql = query.buildSelectSql();
        assertFalse("SQL should NOT contain deleted_at", sql.contains("deleted_at"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void softDeleteCustomValueWithSingleTableShouldWork() {
        SingleTableTemplate<Object> st = new SingleTableTemplate<>(jdbcTemplate, "test_table");
        st.setSoftDeleteValue(-1L);
        st.where("status", 1);

        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(1L);

        st.count();

        verify(jdbcTemplate).queryForObject(anyString(), argThat((Object[] args) -> {
            for (Object arg : args) {
                if (arg instanceof Long && ((Long) arg) == -1L) return true;
            }
            return false;
        }), eq(Number.class));
    }

    // ==================== 4. lock 默认不拼接 ====================

    @Test
    public void defaultLockShouldNotAppearInSql() {
        String sql = query.buildSelectSql();
        assertFalse("Default SQL should not contain FOR UPDATE or FOR SHARE",
                sql.contains("FOR UPDATE") || sql.contains("FOR SHARE"));
    }

    @Test
    public void lockForUpdateShouldAppendForUpdate() {
        query.lockForUpdate();
        query.where("status", 1);

        String sql = query.buildSelectSql();
        assertTrue("SQL should contain FOR UPDATE", sql.contains("FOR UPDATE"));
    }

    @Test
    public void lockForShareShouldAppendForShare() {
        query.lockForShare();
        query.where("status", 1);

        String sql = query.buildSelectSql();
        assertTrue("SQL should contain FOR SHARE", sql.contains("FOR SHARE"));
    }

    // ==================== 5. 基础查询 ====================

    @Test
    public void firstShouldReturnNullWhenNoResult() {
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(Collections.emptyList());

        Object result = query.first(Object.class);
        assertNull(result);
    }

    @Test
    public void existsShouldReturnTrueWhenCountGreaterThanZero() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class)))
                .thenReturn(1L);

        assertTrue(query.exists());
    }

    @Test
    public void existsShouldReturnFalseWhenCountIsZero() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class)))
                .thenReturn(0L);

        assertFalse(query.exists());
    }

    // ==================== 6. 聚合方法参数校验 ====================

    @Test(expected = IllegalArgumentException.class)
    public void maxShouldThrowOnNullField() {
        query.max((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void maxShouldThrowOnEmptyField() {
        query.max("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void minShouldThrowOnNullField() {
        query.min((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void minShouldThrowOnEmptyField() {
        query.min("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void avgShouldThrowOnNullField() {
        query.avg((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sumShouldThrowOnNullField() {
        query.sum((String) null);
    }

    // ==================== 7. 链式调用基础功能 ====================

    @Test
    public void chainWhereAndOrder() {
        String sql = query.where("status", 1)
                .where("type", "active")
                .orderByDesc("created_at")
                .buildSelectSql();

        assertTrue("SQL should contain WHERE", sql.contains("where"));
        assertTrue("SQL should contain ORDER BY", sql.contains("ORDER BY"));
        assertTrue("SQL should contain DESC", sql.contains("DESC"));
        assertTrue("SQL should contain status", sql.contains("status"));
        assertTrue("SQL should contain type", sql.contains("type"));
    }

    @Test
    public void chainWhereLike() {
        String sql = query.whereLike("name", "test")
                .buildSelectSql();

        assertTrue("SQL should contain LIKE", sql.contains("like"));
    }

    @Test
    public void chainWhereIn() {
        String sql = query.whereIn("status", Arrays.asList(1, 2, 3))
                .buildSelectSql();

        assertTrue("SQL should contain IN", sql.contains("in"));
        assertTrue("SQL should have 3 placeholders", sql.chars().filter(c -> c == '?').count() >= 3);
    }

    @Test
    public void chainLimit() {
        String sql = query.where("status", 1)
                .limit(10, 5)
                .buildSelectSql();

        assertTrue("SQL should contain LIMIT", sql.contains("LIMIT"));
        assertTrue("SQL should contain offset", sql.contains("5,10"));
    }

    @Test
    public void chainSelectWithField() {
        String sql = query.select("id, name")
                .buildSelectSql();

        assertTrue("SQL should contain id, name", sql.contains("id, name"));
    }

    @Test
    public void chainJoin() {
        String sql = query.select("u.*, o.total")
                .leftJoin("orders", "u.id", "o.user_id")
                .buildSelectSql();

        assertTrue("SQL should contain LEFT JOIN", sql.contains("LEFT JOIN"));
    }

    @Test
    public void groupByShouldAppearInSql() {
        String sql = query.where("status", 1)
                .groupBy("type")
                .buildSelectSql();

        assertTrue("SQL should contain GROUP BY", sql.contains("GROUP BY"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void chainedIncrementShouldGenerateCorrectSql() {
        query.where("id", 1);
        query.increment("view_count");

        verify(jdbcTemplate).update(
                argThat(sql -> ((String) sql).contains("view_count=view_count+1")),
                any(Object[].class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void chainedDecrementShouldGenerateCorrectSql() {
        query.where("id", 1);
        query.decrement("stock");

        verify(jdbcTemplate).update(
                argThat(sql -> ((String) sql).contains("stock=stock-1")),
                any(Object[].class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void softDeleteInCountSql() {
        query.where("status", 1);

        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(0L);

        query.count();

        verify(jdbcTemplate).queryForObject(
                argThat(sql -> ((String) sql).contains("deleted_at = ?")),
                any(Object[].class),
                eq(Number.class)
        );
    }
}

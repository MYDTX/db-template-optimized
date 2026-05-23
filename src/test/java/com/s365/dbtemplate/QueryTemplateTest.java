package com.s365.dbtemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 测试 QueryTemplate 核心功能：
 * - 聚合方法不修改 selectString
 * - 软删除值可配置
 * - 空结果安全返回 null
 * - lock 默认不拼接
 */
class QueryTemplateTest {

    private JdbcTemplate jdbcTemplate;
    private QueryTemplate<Object> query;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        query = new QueryTemplate<>(jdbcTemplate);
        query.table("test_table");
    }

    // ==================== 1. 聚合方法不修改 selectString ====================

    @Test
    @SuppressWarnings("unchecked")
    void countShouldGenerateCountSql() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(5L);

        query.where("status", 1).count();

        verify(jdbcTemplate).queryForObject(
                argThat(sql -> ((String) sql).contains("COUNT(*)")),
                any(Object[].class),
                eq(Number.class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void countShouldNotAffectSubsequentSelectQueries() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(5L);

        query.where("status", 1);
        query.count();

        String[] capturedSql = new String[1];
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(invocation -> {
                    capturedSql[0] = invocation.getArgument(0);
                    return Collections.emptyList();
                });

        query.first(Object.class);

        assertNotNull(capturedSql[0]);
        assertTrue(capturedSql[0].toUpperCase().startsWith("SELECT"));
        assertFalse(capturedSql[0].toUpperCase().contains("COUNT"));
        assertTrue(capturedSql[0].contains("test_table.*"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void countWithFieldShouldUseCountField() {
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
    void maxShouldNotAffectSubsequentQueries() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(100);

        query.select("id");
        query.max("price");

        String[] capturedSql = new String[1];
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(invocation -> {
                    capturedSql[0] = invocation.getArgument(0);
                    return Collections.emptyList();
                });

        query.first(Object.class);

        assertNotNull(capturedSql[0]);
        assertTrue(capturedSql[0].contains("id"));
        assertFalse(capturedSql[0].toUpperCase().contains("MAX"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void minShouldNotAffectSubsequentQueries() {
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
        assertTrue(capturedSql[0].contains("name"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void avgShouldGenerateAvgSql() {
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
    void sumShouldGenerateSumSql() {
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
    void maxShouldReturnNullWhenNoResult() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(null);

        Number result = query.max("price");

        assertNull(result);
    }

    @Test
    void minShouldReturnNullWhenNoResult() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(null);

        Number result = query.min("price");

        assertNull(result);
    }

    // ==================== 3. 软删除值可配置 ====================

    @Test
    @SuppressWarnings("unchecked")
    void softDeleteCustomValueShouldBePassedAsParam() {
        query.setSoftDeleteValue(-1L);
        query.where("status", 1);

        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Number.class)))
                .thenReturn(1L);

        query.count();

        verify(jdbcTemplate).queryForObject(anyString(), argThat((Object[] args) -> {
            for (Object arg : args) {
                if (arg instanceof Long && ((Long) arg) == -1L) return true;
            }
            return false;
        }), eq(Number.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void softDeleteDefaultValueShouldBeZeroInParams() {
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
    void unUseSoftDeleteShouldRemoveDeletedAtFromSql() {
        query.unUseSoftDelete();
        query.where("status", 1);

        String sql = query.buildSelectSql();
        assertFalse(sql.contains("deleted_at"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void softDeleteCustomValueWithSingleTableShouldWork() {
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
    void defaultLockShouldNotAppearInSql() {
        String sql = query.buildSelectSql();
        assertFalse(sql.contains("FOR UPDATE") || sql.contains("FOR SHARE"));
    }

    @Test
    void lockForUpdateShouldAppendForUpdate() {
        query.lockForUpdate();
        query.where("status", 1);

        String sql = query.buildSelectSql();
        assertTrue(sql.contains("FOR UPDATE"));
    }

    @Test
    void lockForShareShouldAppendForShare() {
        query.lockForShare();
        query.where("status", 1);

        String sql = query.buildSelectSql();
        assertTrue(sql.contains("FOR SHARE"));
    }

    // ==================== 5. 基础查询 ====================

    @Test
    void firstShouldReturnNullWhenNoResult() {
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(Collections.emptyList());

        Object result = query.first(Object.class);
        assertNull(result);
    }

    @Test
    void existsShouldReturnTrueWhenCountGreaterThanZero() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class)))
                .thenReturn(1L);

        assertTrue(query.exists());
    }

    @Test
    void existsShouldReturnFalseWhenCountIsZero() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class)))
                .thenReturn(0L);

        assertFalse(query.exists());
    }

    // ==================== 6. 聚合方法参数校验 ====================

    @Test
    void maxShouldThrowOnNullField() {
        assertThrows(IllegalArgumentException.class, () -> query.max((String) null));
    }

    @Test
    void maxShouldThrowOnEmptyField() {
        assertThrows(IllegalArgumentException.class, () -> query.max(""));
    }

    @Test
    void minShouldThrowOnNullField() {
        assertThrows(IllegalArgumentException.class, () -> query.min((String) null));
    }

    @Test
    void minShouldThrowOnEmptyField() {
        assertThrows(IllegalArgumentException.class, () -> query.min(""));
    }

    @Test
    void avgShouldThrowOnNullField() {
        assertThrows(IllegalArgumentException.class, () -> query.avg((String) null));
    }

    @Test
    void sumShouldThrowOnNullField() {
        assertThrows(IllegalArgumentException.class, () -> query.sum((String) null));
    }

    // ==================== 7. 链式调用基础功能 ====================

    @Test
    void chainWhereAndOrder() {
        String sql = query.where("status", 1)
                .where("type", "active")
                .orderByDesc("created_at")
                .buildSelectSql();

        assertTrue(sql.contains("where"));
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("DESC"));
        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("type"));
    }

    @Test
    void chainWhereLike() {
        String sql = query.whereLike("name", "test")
                .buildSelectSql();

        assertTrue(sql.contains("like"));
    }

    @Test
    void chainWhereIn() {
        String sql = query.whereIn("status", Arrays.asList(1, 2, 3))
                .buildSelectSql();

        assertTrue(sql.contains("in"));
        assertTrue(sql.chars().filter(c -> c == '?').count() >= 3);
    }

    @Test
    void chainLimit() {
        String sql = query.where("status", 1)
                .limit(10, 5)
                .buildSelectSql();

        assertTrue(sql.contains("LIMIT"));
        assertTrue(sql.contains("5,10"));
    }

    @Test
    void chainSelectWithField() {
        String sql = query.select("id, name")
                .buildSelectSql();

        assertTrue(sql.contains("id, name"));
    }

    @Test
    void chainJoin() {
        String sql = query.select("u.*, o.total")
                .leftJoin("orders", "u.id", "o.user_id")
                .buildSelectSql();

        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    void groupByShouldAppearInSql() {
        String sql = query.where("status", 1)
                .groupBy("type")
                .buildSelectSql();

        assertTrue(sql.contains("GROUP BY"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void chainedIncrementShouldGenerateCorrectSql() {
        query.where("id", 1);
        query.increment("view_count");

        verify(jdbcTemplate).update(
                argThat(sql -> ((String) sql).contains("view_count=view_count+1")),
                any(Object[].class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void chainedDecrementShouldGenerateCorrectSql() {
        query.where("id", 1);
        query.decrement("stock");

        verify(jdbcTemplate).update(
                argThat(sql -> ((String) sql).contains("stock=stock-1")),
                any(Object[].class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void softDeleteInCountSql() {
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

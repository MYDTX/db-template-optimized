package com.s365.dbtemplate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;

/**
 * DB Template 自动配置类
 * 当项目中存在 JdbcTemplate Bean 时，自动初始化 Db 类
 */
@Slf4j
@Configuration
@ConditionalOnClass(JdbcTemplate.class)
public class DbTemplateAutoConfiguration {

    private final JdbcTemplate jdbcTemplate;

    public DbTemplateAutoConfiguration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        if (jdbcTemplate != null) {
            Db.init(jdbcTemplate);
        } else {
            log.warn("JdbcTemplate not found in Spring context. Db will not be initialized automatically.");
        }
    }
}

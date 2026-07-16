package com.s365.dbtemplate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DB Template 自动配置类
 * 当项目中存在 JdbcTemplate Bean 时，自动初始化 Db 类
 *
 * 兼容性：
 * - Spring Boot 2.x（javax.annotation 环境）
 * - Spring Boot 3.x（jakarta.annotation 环境）
 * - Spring Boot 4.x（jakarta.annotation 环境，Spring Framework 7）
 * - 纯 Spring 项目（手动调用 Db.init()）
 *
 * 使用 InitializingBean 而非 @PostConstruct，避免依赖 javax/jakarta.annotation-api
 */
@Slf4j
@Configuration
@ConditionalOnClass(JdbcTemplate.class)
public class DbTemplateAutoConfiguration implements InitializingBean {

    private final JdbcTemplate jdbcTemplate;

    public DbTemplateAutoConfiguration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        if (jdbcTemplate != null) {
            Db.init(jdbcTemplate);
            log.debug("Db auto-initialized with JdbcTemplate: {}", jdbcTemplate);
        } else {
            log.warn("JdbcTemplate not found in Spring context. Db will not be initialized automatically.");
        }
    }
}

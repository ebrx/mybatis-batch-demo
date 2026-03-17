package com.example.demo.config;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * MyBatis 补充配置。
 *
 * <p>注册一个 BATCH 模式的 SqlSessionTemplate。
 * 与默认 SIMPLE 模式不同，BATCH 模式下多次调用同一 Mapper 方法时，
 * MyBatis 在底层调用 JDBC addBatch()，flushStatements() 时统一发送，
 * 且该 Template 参与 Spring 事务管理（共享同一 Connection），
 * 可被 @Transactional 正确控制提交/回滚。
 */
@Configuration
public class MyBatisConfig {

    /**
     * 默认 SIMPLE 模式 SqlSessionTemplate（@Primary）。
     * 供普通 Mapper 使用，确保 insert 能拿到生成主键。
     */
    @Bean
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.SIMPLE);
    }

    @Bean(name = "batchSqlSessionTemplate")
    public SqlSessionTemplate batchSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
    }
}

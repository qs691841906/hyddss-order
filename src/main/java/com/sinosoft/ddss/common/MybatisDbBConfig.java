package com.sinosoft.ddss.common;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(basePackages = {"com.sinosoft.ddss.dataDao"}, sqlSessionFactoryRef = "sqlSessionFactory2")
public class MybatisDbBConfig {

    @Autowired
    @Qualifier("greplum")
    private DataSource greplum;

    @Bean(name= "sqlSessionFactory2")
    public SqlSessionFactory sqlSessionFactory2() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(greplum); // 使用ds2数据源, 连接ds2库
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:dataMapper/*.xml"));
        return factoryBean.getObject();

    }
    @Bean(name= "sqlSessionTemplate2")
    public SqlSessionTemplate sqlSessionTemplate2() throws Exception {
        SqlSessionTemplate template = new SqlSessionTemplate(sqlSessionFactory2()); // 使用上面配置的Factory
        return template;
    }
}
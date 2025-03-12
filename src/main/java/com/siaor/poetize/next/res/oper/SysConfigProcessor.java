package com.siaor.poetize.next.res.oper;

import com.siaor.poetize.next.res.norm.exception.SysRuntimeException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义环境变量配置
 *
 * @author Siaor
 * @since 2025-03-11 02:03:16
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class SysConfigProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            //获取数据库配置
            boolean autoInit = "true".equalsIgnoreCase(environment.getProperty("sys.auto-init"));
            String username = environment.getProperty("spring.datasource.username");
            String password = environment.getProperty("spring.datasource.password");
            String url = environment.getProperty("spring.datasource.url");
            String driver = environment.getProperty("spring.datasource.driver-class-name");
            Class.forName(driver);
            if(url == null) {
                throw new SysRuntimeException();
            }

            //连接数据库获取配置数据
            Map<String, Object> map = new HashMap<>();
            try (Connection connection = DriverManager.getConnection(url, username, password)) {
                //初始化数据库
                if(autoInit) initDb(connection);
                //从数据库获取系统环境配置并加载
                try (Statement statement = connection.createStatement()) {
                    try (ResultSet resultSet = statement.executeQuery("SELECT * FROM sys_config")) {
                        while (resultSet.next()) {
                            map.put(resultSet.getString("config_key"), resultSet.getString("config_value"));
                        }
                    }
                }
            }

            MutablePropertySources propertySources = environment.getPropertySources();
            PropertySource<?> source = new MapPropertySource("sys_config", map);
            propertySources.addFirst(source);
        } catch (Exception e) {
            throw new SysRuntimeException(e);
        }
    }

    private void initDb(Connection connection) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbType = metaData.getDatabaseProductName().toLowerCase();
            Statement statement = connection.createStatement();

            if(isInit(statement)){
                System.out.println("【POETIZE-NEXT】数据库已经初始化，将跳过初始化流程。您可以设置auto-init: false，减少启动时不必要的扫描。");
                return;
            }

            System.out.println("【POETIZE-NEXT】首次使用，正在为您初始化数据库信息");
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            populator.addScripts(resolver.getResources("classpath*:db/"+dbType+"/schema.sql"));
            populator.addScripts(resolver.getResources("classpath*:db/"+dbType+"/data.sql"));
            populator.populate(connection);
            System.out.println("【POETIZE-NEXT】数据库初始化完成！");
        } catch (SQLException e) {
            System.out.println("【POETIZE-NEXT】数据库连接失败，请检查数据库连接配置！");
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.out.println("【POETIZE-NEXT】加载初始化脚本失败！请检查是否为已支持的数据库类型");
            throw new RuntimeException(e);
        }

    }

    private boolean isInit(Statement statement){
        try {
            ResultSet resultSet = statement.executeQuery("SELECT count(1) FROM sys_update_log");
            return resultSet.next();
        }catch (Exception e){
            return false;
        }
    }
}

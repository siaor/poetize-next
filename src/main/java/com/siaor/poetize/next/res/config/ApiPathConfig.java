package com.siaor.poetize.next.res.config;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 接口路径配置
 *
 * @author Siaor
 * @since 2025-03-11 02:37:37
 */
@Component
public class ApiPathConfig implements WebMvcConfigurer {

    /**
     * 接口统一前缀
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));
    }

}

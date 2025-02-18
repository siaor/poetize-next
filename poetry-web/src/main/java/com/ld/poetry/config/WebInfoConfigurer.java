package com.ld.poetry.config;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class WebInfoConfigurer implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new WebInfoHandlerInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login", "/admin/**", "/webInfo/getWebInfo", "/webInfo/updateWebInfo", "/res/**", "/js/**", "/css/**", "/img/**", "/fonts/**", "index.html", "logo.svg", "/im/**");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        //接口增加统一前缀
        configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));
    }
}

package com.siaor.poetize.next.res.config;

import com.siaor.poetize.next.res.oper.WebInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 网络配置
 *
 * @author Siaor
 * @since 2025-03-11 02:35:59
 */
@Component
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new WebInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login", "/admin/**", "/webInfo/getWebInfo", "/webInfo/updateWebInfo", "/res/**", "/js/**", "/css/**", "/img/**", "/fonts/**", "index.html", "logo.svg", "/im/**");
    }

}

package com.siaor.poetize.next.res.oper;

import com.alibaba.fastjson.JSON;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.ActCode;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.res.utils.storage.FileFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 网络过滤器
 *
 * @author Siaor
 * @since 2025-03-11 02:39:52
 */
@Component
public class WebFilter extends OncePerRequestFilter {

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private FileFilter fileFilter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!"OPTIONS".equals(request.getMethod())) {
            try {
                commonQuery.saveHistory(PoetryUtil.getIpAddr(request));
            } catch (Exception e) {
            }

            if (fileFilter.doFilterFile(request, response)) {
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JSON.toJSONString(ActResult.fail(ActCode.PARAMETER_ERROR.getCode(), ActCode.PARAMETER_ERROR.getMsg())));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

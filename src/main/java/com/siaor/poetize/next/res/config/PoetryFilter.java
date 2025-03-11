package com.siaor.poetize.next.res.config;

import com.alibaba.fastjson.JSON;
import com.siaor.poetize.next.res.enums.CodeMsg;
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

@Component
public class PoetryFilter extends OncePerRequestFilter {

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
                response.getWriter().write(JSON.toJSONString(PoetryResult.fail(CodeMsg.PARAMETER_ERROR.getCode(), CodeMsg.PARAMETER_ERROR.getMsg())));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

package com.siaor.poetize.next.res.config;

import com.alibaba.fastjson.JSON;
import com.siaor.poetize.next.repo.po.WebInfoPO;
import com.siaor.poetize.next.res.enums.CodeMsg;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.res.utils.cache.PoetryCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;


public class WebInfoHandlerInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        WebInfoPO webInfoPO = (WebInfoPO) PoetryCache.get(CommonConst.WEB_INFO);
        if (webInfoPO == null || !webInfoPO.getStatus()) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(PoetryResult.fail(CodeMsg.SYSTEM_REPAIR.getCode(), CodeMsg.SYSTEM_REPAIR.getMsg())));
            return false;
        } else {
            return true;
        }
    }
}

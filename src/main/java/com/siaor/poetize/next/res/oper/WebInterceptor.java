package com.siaor.poetize.next.res.oper;

import com.alibaba.fastjson.JSON;
import com.siaor.poetize.next.res.repo.po.WebInfoPO;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.ActCode;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.cache.SysCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 网络拦截器
 *
 * @author Siaor
 * @since 2025-03-11 02:48:56
 */
public class WebInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        WebInfoPO webInfoPO = (WebInfoPO) SysCache.get(CommonConst.WEB_INFO);
        if (webInfoPO == null || !webInfoPO.getStatus()) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(ActResult.fail(ActCode.SYSTEM_REPAIR.getCode(), ActCode.SYSTEM_REPAIR.getMsg())));
            return false;
        } else {
            return true;
        }
    }
}

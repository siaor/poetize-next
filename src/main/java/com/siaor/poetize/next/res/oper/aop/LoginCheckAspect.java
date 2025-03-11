package com.siaor.poetize.next.res.oper.aop;

import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.po.UserPO;
import com.siaor.poetize.next.res.norm.ActCode;
import com.siaor.poetize.next.res.norm.SysEnum;
import com.siaor.poetize.next.res.norm.exception.SysLoginException;
import com.siaor.poetize.next.res.norm.exception.SysRuntimeException;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.res.repo.cache.SysCache;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Aspect
@Component
@Order(0)
@Slf4j
public class LoginCheckAspect {

    @Around("@annotation(loginCheck)")
    public Object around(ProceedingJoinPoint joinPoint, LoginCheck loginCheck) throws Throwable {
        String token = PoetryUtil.getToken();
        if (!StringUtils.hasText(token)) {
            throw new SysLoginException(ActCode.NOT_LOGIN.getMsg());
        }

        UserPO userPO = (UserPO) SysCache.get(token);

        if (userPO == null) {
            throw new SysLoginException(ActCode.LOGIN_EXPIRED.getMsg());
        }

        if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
            if (loginCheck.value() == SysEnum.USER_TYPE_ADMIN.getCode() || loginCheck.value() == SysEnum.USER_TYPE_AUTH.getCode()) {
                return ActResult.fail("请输入管理员账号！");
            }
        } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
            log.info("请求IP：" + PoetryUtil.getIpAddr(PoetryUtil.getRequest()));
            if (loginCheck.value() == SysEnum.USER_TYPE_ADMIN.getCode() && userPO.getId().intValue() != CommonConst.ADMIN_USER_ID) {
                return ActResult.fail("请输入管理员账号！");
            }
        } else {
            throw new SysLoginException(ActCode.NOT_LOGIN.getMsg());
        }

        if (loginCheck.value() < userPO.getUserType()) {
            throw new SysRuntimeException("权限不足！");
        }

        //重置过期时间
        String userId = userPO.getId().toString();
        boolean flag1 = false;
        if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
            flag1 = SysCache.get(CommonConst.USER_TOKEN_INTERVAL + userId) == null;
        } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
            flag1 = SysCache.get(CommonConst.ADMIN_TOKEN_INTERVAL + userId) == null;
        }

        if (flag1) {
            synchronized (userId.intern()) {
                boolean flag2 = false;
                if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
                    flag2 = SysCache.get(CommonConst.USER_TOKEN_INTERVAL + userId) == null;
                } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
                    flag2 = SysCache.get(CommonConst.ADMIN_TOKEN_INTERVAL + userId) == null;
                }

                if (flag2) {
                    SysCache.put(token, userPO, CommonConst.TOKEN_EXPIRE);
                    if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
                        SysCache.put(CommonConst.USER_TOKEN + userId, token, CommonConst.TOKEN_EXPIRE);
                        SysCache.put(CommonConst.USER_TOKEN_INTERVAL + userId, token, CommonConst.TOKEN_INTERVAL);
                    } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
                        SysCache.put(CommonConst.ADMIN_TOKEN + userId, token, CommonConst.TOKEN_EXPIRE);
                        SysCache.put(CommonConst.ADMIN_TOKEN_INTERVAL + userId, token, CommonConst.TOKEN_INTERVAL);
                    }
                }
            }
        }
        return joinPoint.proceed();
    }
}

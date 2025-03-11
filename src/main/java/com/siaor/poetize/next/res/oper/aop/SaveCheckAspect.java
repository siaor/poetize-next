package com.siaor.poetize.next.res.oper.aop;

import com.siaor.poetize.next.res.repo.po.UserPO;
import com.siaor.poetize.next.res.norm.exception.SysRuntimeException;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.cache.SysCache;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;


@Aspect
@Component
@Order(1)
@Slf4j
public class SaveCheckAspect {

    @Around("@annotation(saveCheck)")
    public Object around(ProceedingJoinPoint joinPoint, SaveCheck saveCheck) throws Throwable {
        boolean flag = false;

        String token = PoetryUtil.getToken();
        if (StringUtils.hasText(token)) {
            UserPO userPO = (UserPO) SysCache.get(token);
            if (userPO != null) {
                if (userPO.getId().intValue() == PoetryUtil.getAdminUser().getId().intValue()) {
                    return joinPoint.proceed();
                }

                AtomicInteger atomicInteger = (AtomicInteger) SysCache.get(CommonConst.SAVE_COUNT_USER_ID + userPO.getId().toString());
                if (atomicInteger == null) {
                    atomicInteger = new AtomicInteger();
                    SysCache.put(CommonConst.SAVE_COUNT_USER_ID + userPO.getId().toString(), atomicInteger, CommonConst.SAVE_EXPIRE);
                }
                int userIdCount = atomicInteger.getAndIncrement();
                if (userIdCount >= CommonConst.SAVE_MAX_COUNT) {
                    log.info("用户保存超限：" + userPO.getId().toString() + "，次数：" + userIdCount);
                    flag = true;
                }
            }
        }

        String ip = PoetryUtil.getIpAddr(PoetryUtil.getRequest());
        AtomicInteger atomic = (AtomicInteger) SysCache.get(CommonConst.SAVE_COUNT_IP + ip);
        if (atomic == null) {
            atomic = new AtomicInteger();
            SysCache.put(CommonConst.SAVE_COUNT_IP + ip, atomic, CommonConst.SAVE_EXPIRE);
        }
        int ipCount = atomic.getAndIncrement();
        if (ipCount > CommonConst.SAVE_MAX_COUNT) {
            log.info("IP保存超限：" + ip + "，次数：" + ipCount);
            flag = true;
        }

        if (flag) {
            throw new SysRuntimeException("今日提交次数已用尽，请一天后再来！");
        }

        return joinPoint.proceed();
    }
}

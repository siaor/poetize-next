package com.siaor.poetize.next.res.utils.storage;

import com.siaor.poetize.next.repo.po.UserPO;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.res.utils.cache.PoetryCache;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FileFilter {

    private final AntPathMatcher matcher = new AntPathMatcher();

    public boolean doFilterFile(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        if (matcher.match("/resource/upload", httpServletRequest.getRequestURI())) {
            String token = PoetryUtil.getToken();
            if (StringUtils.hasText(token)) {
                UserPO userPO = (UserPO) PoetryCache.get(token);

                if (userPO != null) {
                    if (userPO.getId().intValue() == PoetryUtil.getAdminUser().getId().intValue()) {
                        return false;
                    }

                    AtomicInteger atomicInteger = (AtomicInteger) PoetryCache.get(CommonConst.SAVE_COUNT_USER_ID + userPO.getId().toString());
                    if (atomicInteger == null) {
                        atomicInteger = new AtomicInteger();
                        PoetryCache.put(CommonConst.SAVE_COUNT_USER_ID + userPO.getId().toString(), atomicInteger, CommonConst.SAVE_EXPIRE);
                    }
                    int userIdCount = atomicInteger.getAndIncrement();

                    String ip = PoetryUtil.getIpAddr(PoetryUtil.getRequest());
                    AtomicInteger atomic = (AtomicInteger) PoetryCache.get(CommonConst.SAVE_COUNT_IP + ip);
                    if (atomic == null) {
                        atomic = new AtomicInteger();
                        PoetryCache.put(CommonConst.SAVE_COUNT_IP + ip, atomic, CommonConst.SAVE_EXPIRE);
                    }
                    int ipCount = atomic.getAndIncrement();

                    return userIdCount >= CommonConst.SAVE_MAX_COUNT || ipCount >= CommonConst.SAVE_MAX_COUNT;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}

package com.siaor.poetize.next.res.task;

import com.siaor.poetize.next.res.repo.mapper.HistoryInfoMapper;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.cache.SysCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 系统定时任务
 *
 * @author Siaor
 * @since 2025-03-11 02:27:33
 */
@Component
@EnableScheduling
@Slf4j
public class SysScheduleTask {

    @Autowired
    private HistoryInfoMapper historyInfoMapper;

    /**
     * 凌晨刷新访问统计数据到缓存
     *
     * @author Siaor
     * @since 2025-03-11 02:27:55
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanIpHistory() {
        CopyOnWriteArraySet<String> ipHistory = (CopyOnWriteArraySet<String>) SysCache.get(CommonConst.IP_HISTORY);
        if (ipHistory == null) {
            ipHistory = new CopyOnWriteArraySet<>();
            SysCache.put(CommonConst.IP_HISTORY, ipHistory);
        }
        ipHistory.clear();

        SysCache.remove(CommonConst.IP_HISTORY_STATISTICS);
        Map<String, Object> history = new HashMap<>();
        history.put(CommonConst.IP_HISTORY_PROVINCE, historyInfoMapper.getHistoryByProvince());
        history.put(CommonConst.IP_HISTORY_IP, historyInfoMapper.getHistoryByIp());
        history.put(CommonConst.IP_HISTORY_HOUR, historyInfoMapper.getHistoryBy24Hour());
        history.put(CommonConst.IP_HISTORY_COUNT, historyInfoMapper.getHistoryCount());
        SysCache.put(CommonConst.IP_HISTORY_STATISTICS, history);
    }
}

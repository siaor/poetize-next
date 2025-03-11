package com.siaor.poetize.next.res.oper;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.res.repo.mapper.HistoryInfoMapper;
import com.siaor.poetize.next.res.repo.mapper.WebInfoMapper;
import com.siaor.poetize.next.res.repo.po.FamilyPO;
import com.siaor.poetize.next.res.repo.po.HistoryInfoPO;
import com.siaor.poetize.next.res.repo.po.UserPO;
import com.siaor.poetize.next.res.repo.po.WebInfoPO;
import com.siaor.poetize.next.res.utils.TioUtil;
import com.siaor.poetize.next.res.oper.im.TioWebsocketStarter;
import com.siaor.poetize.next.pow.FamilyPow;
import com.siaor.poetize.next.pow.UserPow;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.cache.SysCache;
import com.siaor.poetize.next.res.norm.SysEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Component
public class SysRunner implements ApplicationRunner {

    @Value("${store.type}")
    private String defaultType;

    @Autowired
    private WebInfoMapper webInfoMapper;

    @Autowired
    private UserPow userPow;

    @Autowired
    private FamilyPow familyPow;

    @Autowired
    private HistoryInfoMapper historyInfoMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LambdaQueryChainWrapper<WebInfoPO> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
        List<WebInfoPO> list = wrapper.list();
        if (!CollectionUtils.isEmpty(list)) {
            list.get(0).setDefaultStoreType(defaultType);
            SysCache.put(CommonConst.WEB_INFO, list.get(0));
        }

        UserPO admin = userPow.lambdaQuery().eq(UserPO::getUserType, SysEnum.USER_TYPE_ADMIN.getCode()).one();
        SysCache.put(CommonConst.ADMIN, admin);

        FamilyPO familyPO = familyPow.lambdaQuery().eq(FamilyPO::getUserId, admin.getId()).one();
        SysCache.put(CommonConst.ADMIN_FAMILY, familyPO);

        List<HistoryInfoPO> infoList = new LambdaQueryChainWrapper<>(historyInfoMapper)
                .select(HistoryInfoPO::getIp, HistoryInfoPO::getUserId)
                .ge(HistoryInfoPO::getCreateTime, LocalDateTime.now().with(LocalTime.MIN))
                .list();

        SysCache.put(CommonConst.IP_HISTORY, new CopyOnWriteArraySet<>(infoList.stream().map(info -> info.getIp() + (info.getUserId() != null ? "_" + info.getUserId().toString() : "")).collect(Collectors.toList())));

        Map<String, Object> history = new HashMap<>();
        history.put(CommonConst.IP_HISTORY_PROVINCE, historyInfoMapper.getHistoryByProvince());
        history.put(CommonConst.IP_HISTORY_IP, historyInfoMapper.getHistoryByIp());
        history.put(CommonConst.IP_HISTORY_HOUR, historyInfoMapper.getHistoryBy24Hour());
        history.put(CommonConst.IP_HISTORY_COUNT, historyInfoMapper.getHistoryCount());
        SysCache.put(CommonConst.IP_HISTORY_STATISTICS, history);

        TioUtil.buildTio();
        TioWebsocketStarter websocketStarter = TioUtil.getTio();
        if (websocketStarter != null) {
            websocketStarter.start();
        }
    }
}

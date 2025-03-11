package com.siaor.poetize.next.app.api.sys;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.res.aop.LoginCheck;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.repo.mapper.HistoryInfoMapper;
import com.siaor.poetize.next.repo.po.HistoryInfoPO;
import com.siaor.poetize.next.repo.po.UserPO;
import com.siaor.poetize.next.repo.po.WebInfoPO;
import com.siaor.poetize.next.pow.WebInfoPow;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.utils.cache.PoetryCache;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 网站信息表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/webInfo")
public class WebInfoApi {

    @Value("${store.type}")
    private String defaultType;

    @Autowired
    private WebInfoPow webInfoPow;

    @Autowired
    private HistoryInfoMapper historyInfoMapper;

    @Autowired
    private CommonQuery commonQuery;


    /**
     * 更新网站信息
     */
    @LoginCheck(0)
    @PostMapping("/updateWebInfo")
    public PoetryResult<WebInfoPO> updateWebInfo(@RequestBody WebInfoPO webInfoPO) {
        webInfoPow.updateById(webInfoPO);

        LambdaQueryChainWrapper<WebInfoPO> wrapper = new LambdaQueryChainWrapper<>(webInfoPow.getBaseMapper());
        List<WebInfoPO> list = wrapper.list();
        if (!CollectionUtils.isEmpty(list)) {
            list.get(0).setDefaultStoreType(defaultType);
            PoetryCache.put(CommonConst.WEB_INFO, list.get(0));
        }
        return PoetryResult.success();
    }


    /**
     * 获取网站信息
     */
    @GetMapping("/getWebInfo")
    public PoetryResult<WebInfoPO> getWebInfo() {
        WebInfoPO webInfoPO = (WebInfoPO) PoetryCache.get(CommonConst.WEB_INFO);
        if (webInfoPO != null) {
            WebInfoPO result = new WebInfoPO();
            BeanUtils.copyProperties(webInfoPO, result);
            result.setRandomAvatar(null);
            result.setRandomName(null);
            result.setWaifuJson(null);

            webInfoPO.setHistoryAllCount(((Long) ((Map<String, Object>) PoetryCache.get(CommonConst.IP_HISTORY_STATISTICS)).get(CommonConst.IP_HISTORY_COUNT)).toString());
            webInfoPO.setHistoryDayCount(Integer.toString(((List<Map<String, Object>>) ((Map<String, Object>) PoetryCache.get(CommonConst.IP_HISTORY_STATISTICS)).get(CommonConst.IP_HISTORY_HOUR)).size()));
            return PoetryResult.success(result);
        }
        return PoetryResult.success();
    }

    /**
     * 获取网站统计信息
     */
    @LoginCheck(0)
    @GetMapping("/getHistoryInfo")
    public PoetryResult<Map<String, Object>> getHistoryInfo() {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> history = (Map<String, Object>) PoetryCache.get(CommonConst.IP_HISTORY_STATISTICS);
        List<HistoryInfoPO> infoList = new LambdaQueryChainWrapper<>(historyInfoMapper)
                .select(HistoryInfoPO::getIp, HistoryInfoPO::getUserId, HistoryInfoPO::getNation, HistoryInfoPO::getProvince, HistoryInfoPO::getCity)
                .ge(HistoryInfoPO::getCreateTime, LocalDateTime.now().with(LocalTime.MIN))
                .list();

        result.put(CommonConst.IP_HISTORY_PROVINCE, history.get(CommonConst.IP_HISTORY_PROVINCE));
        result.put(CommonConst.IP_HISTORY_IP, history.get(CommonConst.IP_HISTORY_IP));
        result.put(CommonConst.IP_HISTORY_COUNT, history.get(CommonConst.IP_HISTORY_COUNT));
        List<Map<String, Object>> ipHistoryCount = (List<Map<String, Object>>) history.get(CommonConst.IP_HISTORY_HOUR);
        result.put("ip_count_yest", ipHistoryCount.stream().map(m -> m.get("ip")).distinct().count());
        result.put("username_yest", ipHistoryCount.stream().map(m -> {
            Object userId = m.get("user_id");
            if (userId != null) {
                UserPO userPO = commonQuery.getUser(Integer.valueOf(userId.toString()));
                if (userPO != null) {
                    Map<String, String> userInfo = new HashMap<>();
                    userInfo.put("avatar", userPO.getAvatar());
                    userInfo.put("username", userPO.getUsername());
                    return userInfo;
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList()));
        result.put("ip_count_today", infoList.stream().map(HistoryInfoPO::getIp).distinct().count());
        result.put("username_today", infoList.stream().map(m -> {
            Integer userId = m.getUserId();
            if (userId != null) {
                UserPO userPO = commonQuery.getUser(userId);
                if (userPO != null) {
                    Map<String, String> userInfo = new HashMap<>();
                    userInfo.put("avatar", userPO.getAvatar());
                    userInfo.put("username", userPO.getUsername());
                    return userInfo;
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList()));

        List<Map<String, Object>> list = infoList.stream()
                .map(HistoryInfoPO::getProvince).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("province", entry.getKey());
                    map.put("num", entry.getValue());
                    return map;
                })
                .sorted((o1, o2) -> Long.valueOf(o2.get("num").toString()).compareTo(Long.valueOf(o1.get("num").toString())))
                .collect(Collectors.toList());

        result.put("province_today", list);

        return PoetryResult.success(result);
    }

    /**
     * 获取赞赏
     */
    @GetMapping("/getAdmire")
    public PoetryResult<List<UserPO>> getAdmire() {
        return PoetryResult.success(commonQuery.getAdmire());
    }

    /**
     * 获取看板娘消息
     */
    @GetMapping("/getWaifuJson")
    public String getWaifuJson() {
        WebInfoPO webInfoPO = (WebInfoPO) PoetryCache.get(CommonConst.WEB_INFO);
        if (webInfoPO != null && StringUtils.hasText(webInfoPO.getWaifuJson())) {
            return webInfoPO.getWaifuJson();
        }
        return "{}";
    }
}


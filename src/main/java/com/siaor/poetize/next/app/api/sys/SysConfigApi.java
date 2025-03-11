package com.siaor.poetize.next.app.api.sys;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.res.aop.LoginCheck;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.repo.po.SysConfigPO;
import com.siaor.poetize.next.res.enums.SysEnum;
import com.siaor.poetize.next.pow.SysConfigPow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 参数配置表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2024-03-23
 */
@RestController
@RequestMapping("/sysConfig")
public class SysConfigApi {

    @Autowired
    private SysConfigPow sysConfigPow;

    /**
     * 查询系统参数
     */
    @GetMapping("/listSysConfig")
    public PoetryResult<Map<String, String>> listSysConfig() {
        LambdaQueryChainWrapper<SysConfigPO> wrapper = new LambdaQueryChainWrapper<>(sysConfigPow.getBaseMapper());
        List<SysConfigPO> sysConfigs = wrapper.eq(SysConfigPO::getConfigType, Integer.toString(SysEnum.SYS_CONFIG_PUBLIC.getCode()))
                .list();
        Map<String, String> collect = sysConfigs.stream().collect(Collectors.toMap(SysConfigPO::getConfigKey, SysConfigPO::getConfigValue));
        return PoetryResult.success(collect);
    }

    /**
     * 保存或更新
     */
    @PostMapping("/saveOrUpdateConfig")
    @LoginCheck(0)
    public PoetryResult saveConfig(@RequestBody SysConfigPO sysConfigPO) {
        if (!StringUtils.hasText(sysConfigPO.getConfigName()) ||
                !StringUtils.hasText(sysConfigPO.getConfigKey()) ||
                !StringUtils.hasText(sysConfigPO.getConfigType())) {
            return PoetryResult.fail("请完善所有配置信息！");
        }
        String configType = sysConfigPO.getConfigType();
        if (!Integer.toString(SysEnum.SYS_CONFIG_PUBLIC.getCode()).equals(configType) &&
                !Integer.toString(SysEnum.SYS_CONFIG_PRIVATE.getCode()).equals(configType)) {
            return PoetryResult.fail("配置类型不正确！");
        }
        sysConfigPow.saveOrUpdate(sysConfigPO);
        return PoetryResult.success();
    }

    /**
     * 删除
     */
    @GetMapping("/deleteConfig")
    @LoginCheck(0)
    public PoetryResult deleteConfig(@RequestParam("id") Integer id) {
        sysConfigPow.removeById(id);
        return PoetryResult.success();
    }

    /**
     * 查询
     */
    @GetMapping("/listConfig")
    @LoginCheck(0)
    public PoetryResult<List<SysConfigPO>> listConfig() {
        return PoetryResult.success(new LambdaQueryChainWrapper<>(sysConfigPow.getBaseMapper()).list());
    }
}

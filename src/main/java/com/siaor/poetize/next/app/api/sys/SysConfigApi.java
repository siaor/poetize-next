package com.siaor.poetize.next.app.api.sys;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.repo.po.SysConfigPO;
import com.siaor.poetize.next.res.norm.SysEnum;
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
    public ActResult<Map<String, String>> listSysConfig() {
        LambdaQueryChainWrapper<SysConfigPO> wrapper = new LambdaQueryChainWrapper<>(sysConfigPow.getBaseMapper());
        List<SysConfigPO> sysConfigs = wrapper.eq(SysConfigPO::getConfigType, Integer.toString(SysEnum.SYS_CONFIG_PUBLIC.getCode()))
                .list();
        Map<String, String> collect = sysConfigs.stream().collect(Collectors.toMap(SysConfigPO::getConfigKey, SysConfigPO::getConfigValue));
        return ActResult.success(collect);
    }

    /**
     * 保存或更新
     */
    @PostMapping("/saveOrUpdateConfig")
    @LoginCheck(0)
    public ActResult saveConfig(@RequestBody SysConfigPO sysConfigPO) {
        if (!StringUtils.hasText(sysConfigPO.getConfigName()) ||
                !StringUtils.hasText(sysConfigPO.getConfigKey()) ||
                !StringUtils.hasText(sysConfigPO.getConfigType())) {
            return ActResult.fail("请完善所有配置信息！");
        }
        String configType = sysConfigPO.getConfigType();
        if (!Integer.toString(SysEnum.SYS_CONFIG_PUBLIC.getCode()).equals(configType) &&
                !Integer.toString(SysEnum.SYS_CONFIG_PRIVATE.getCode()).equals(configType)) {
            return ActResult.fail("配置类型不正确！");
        }
        sysConfigPow.saveOrUpdate(sysConfigPO);
        return ActResult.success();
    }

    /**
     * 删除
     */
    @GetMapping("/deleteConfig")
    @LoginCheck(0)
    public ActResult deleteConfig(@RequestParam("id") Integer id) {
        sysConfigPow.removeById(id);
        return ActResult.success();
    }

    /**
     * 查询
     */
    @GetMapping("/listConfig")
    @LoginCheck(0)
    public ActResult<List<SysConfigPO>> listConfig() {
        return ActResult.success(new LambdaQueryChainWrapper<>(sysConfigPow.getBaseMapper()).list());
    }
}

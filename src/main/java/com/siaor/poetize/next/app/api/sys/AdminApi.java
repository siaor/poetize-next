package com.siaor.poetize.next.app.api.sys;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.repo.mapper.TreeHoleMapper;
import com.siaor.poetize.next.res.repo.mapper.WebInfoMapper;
import com.siaor.poetize.next.res.repo.po.TreeHolePO;
import com.siaor.poetize.next.res.repo.po.WebInfoPO;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 后台 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/admin")
public class AdminApi {

    @Autowired
    private WebInfoMapper webInfoMapper;

    @Autowired
    private TreeHoleMapper treeHoleMapper;

    /**
     * 获取网站信息
     */
    @GetMapping("/webInfo/getAdminWebInfo")
    @LoginCheck(0)
    public ActResult<WebInfoPO> getWebInfo() {
        LambdaQueryChainWrapper<WebInfoPO> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
        List<WebInfoPO> list = wrapper.list();
        if (!CollectionUtils.isEmpty(list)) {
            return ActResult.success(list.get(0));
        } else {
            return ActResult.success();
        }
    }

    /**
     * Boss查询树洞
     */
    @PostMapping("/treeHole/boss/list")
    @LoginCheck(0)
    public ActResult<Page> listBossTreeHole(@RequestBody BaseRequestVO baseRequestVO) {
        LambdaQueryChainWrapper<TreeHolePO> wrapper = new LambdaQueryChainWrapper<>(treeHoleMapper);
        wrapper.orderByDesc(TreeHolePO::getCreateTime).page(baseRequestVO);
        return ActResult.success(baseRequestVO);
    }
}

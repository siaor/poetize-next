package com.siaor.poetize.next.app.api.blog;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.oper.aop.SaveCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.mapper.ResourcePathMapper;
import com.siaor.poetize.next.res.repo.po.ResourcePathPO;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.app.vo.ResourcePathVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 资源聚合里的友链，其他接口在ResourceAggregationController
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/webInfo")
public class FriendApi {

    @Autowired
    private ResourcePathMapper resourcePathMapper;

    /**
     * 保存友链
     */
    @LoginCheck
    @PostMapping("/saveFriend")
    @SaveCheck
    public ActResult saveFriend(@RequestBody ResourcePathVO resourcePathVO) {
        if (!StringUtils.hasText(resourcePathVO.getTitle()) || !StringUtils.hasText(resourcePathVO.getCover()) ||
                !StringUtils.hasText(resourcePathVO.getUrl()) || !StringUtils.hasText(resourcePathVO.getIntroduction())) {
            return ActResult.fail("信息不全！");
        }
        ResourcePathPO friend = new ResourcePathPO();
        friend.setClassify(CommonConst.DEFAULT_FRIEND);
        friend.setTitle(resourcePathVO.getTitle());
        friend.setIntroduction(resourcePathVO.getIntroduction());
        friend.setCover(resourcePathVO.getCover());
        friend.setUrl(resourcePathVO.getUrl());
        friend.setRemark(PoetryUtil.getUserId().toString());
        friend.setType(CommonConst.RESOURCE_PATH_TYPE_FRIEND);
        friend.setStatus(Boolean.FALSE);
        resourcePathMapper.insert(friend);
        return ActResult.success();
    }

    /**
     * 查询友链
     */
    @GetMapping("/listFriend")
    public ActResult<Map<String, List<ResourcePathVO>>> listFriend() {
        LambdaQueryChainWrapper<ResourcePathPO> wrapper = new LambdaQueryChainWrapper<>(resourcePathMapper);
        List<ResourcePathPO> resourcePathPOS = wrapper.eq(ResourcePathPO::getType, CommonConst.RESOURCE_PATH_TYPE_FRIEND)
                .eq(ResourcePathPO::getStatus, Boolean.TRUE)
                .orderByAsc(ResourcePathPO::getCreateTime)
                .list();
        Map<String, List<ResourcePathVO>> collect = new HashMap<>();
        if (!CollectionUtils.isEmpty(resourcePathPOS)) {
            collect = resourcePathPOS.stream().map(rp -> {
                ResourcePathVO resourcePathVO = new ResourcePathVO();
                BeanUtils.copyProperties(rp, resourcePathVO);
                return resourcePathVO;
            }).collect(Collectors.groupingBy(ResourcePathVO::getClassify));
        }
        return ActResult.success(collect);
    }
}

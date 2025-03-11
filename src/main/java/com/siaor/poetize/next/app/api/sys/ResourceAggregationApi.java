package com.siaor.poetize.next.app.api.sys;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.res.aop.LoginCheck;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.repo.mapper.ResourcePathMapper;
import com.siaor.poetize.next.repo.po.ResourcePathPO;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import com.siaor.poetize.next.app.vo.ResourcePathVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 资源聚合 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/webInfo")
public class ResourceAggregationApi {

    @Autowired
    private ResourcePathMapper resourcePathMapper;

    /**
     * 保存
     */
    @LoginCheck(0)
    @PostMapping("/saveResourcePath")
    public PoetryResult saveResourcePath(@RequestBody ResourcePathVO resourcePathVO) {
        if (!StringUtils.hasText(resourcePathVO.getTitle()) || !StringUtils.hasText(resourcePathVO.getType())) {
            return PoetryResult.fail("标题和资源类型不能为空！");
        }
        if (CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO.equals(resourcePathVO.getType())) {
            resourcePathVO.setRemark(PoetryUtil.getAdminUser().getId().toString());
        }
        ResourcePathPO resourcePathPO = new ResourcePathPO();
        BeanUtils.copyProperties(resourcePathVO, resourcePathPO);
        resourcePathMapper.insert(resourcePathPO);
        return PoetryResult.success();
    }

    /**
     * 删除
     */
    @GetMapping("/deleteResourcePath")
    @LoginCheck(0)
    public PoetryResult deleteResourcePath(@RequestParam("id") Integer id) {
        resourcePathMapper.deleteById(id);
        return PoetryResult.success();
    }

    /**
     * 更新
     */
    @PostMapping("/updateResourcePath")
    @LoginCheck(0)
    public PoetryResult updateResourcePath(@RequestBody ResourcePathVO resourcePathVO) {
        if (!StringUtils.hasText(resourcePathVO.getTitle()) || !StringUtils.hasText(resourcePathVO.getType())) {
            return PoetryResult.fail("标题和资源类型不能为空！");
        }
        if (resourcePathVO.getId() == null) {
            return PoetryResult.fail("Id不能为空！");
        }
        if (CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO.equals(resourcePathVO.getType())) {
            resourcePathVO.setRemark(PoetryUtil.getAdminUser().getId().toString());
        }
        ResourcePathPO resourcePathPO = new ResourcePathPO();
        BeanUtils.copyProperties(resourcePathVO, resourcePathPO);
        resourcePathMapper.updateById(resourcePathPO);
        return PoetryResult.success();
    }


    /**
     * 查询资源
     */
    @PostMapping("/listResourcePath")
    public PoetryResult<Page> listResourcePath(@RequestBody BaseRequestVO baseRequestVO) {
        LambdaQueryChainWrapper<ResourcePathPO> wrapper = new LambdaQueryChainWrapper<>(resourcePathMapper);
        wrapper.eq(StringUtils.hasText(baseRequestVO.getResourceType()), ResourcePathPO::getType, baseRequestVO.getResourceType());
        wrapper.eq(StringUtils.hasText(baseRequestVO.getClassify()), ResourcePathPO::getClassify, baseRequestVO.getClassify());

        Integer userId = PoetryUtil.getUserId();
        if (!PoetryUtil.getAdminUser().getId().equals(userId)) {
            wrapper.eq(ResourcePathPO::getStatus, Boolean.TRUE);
        } else {
            wrapper.eq(baseRequestVO.getStatus() != null, ResourcePathPO::getStatus, baseRequestVO.getStatus());
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setColumn(StringUtils.hasText(baseRequestVO.getOrder()) ? StrUtil.toUnderlineCase(baseRequestVO.getOrder()) : "create_time");
        orderItem.setAsc(!baseRequestVO.isDesc());
        List<OrderItem> orderItemList = new ArrayList<>();
        orderItemList.add(orderItem);
        baseRequestVO.setOrders(orderItemList);

        wrapper.page(baseRequestVO);

        List<ResourcePathPO> resourcePaths = baseRequestVO.getRecords();
        if (!CollectionUtils.isEmpty(resourcePaths)) {
            List<ResourcePathVO> resourcePathVOs = resourcePaths.stream().map(rp -> {
                ResourcePathVO resourcePathVO = new ResourcePathVO();
                BeanUtils.copyProperties(rp, resourcePathVO);
                return resourcePathVO;
            }).collect(Collectors.toList());
            baseRequestVO.setRecords(resourcePathVOs);
        }
        return PoetryResult.success(baseRequestVO);
    }
}

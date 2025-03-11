package com.siaor.poetize.next.app.api.blog;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.mapper.ResourcePathMapper;
import com.siaor.poetize.next.res.repo.po.ResourcePathPO;
import com.siaor.poetize.next.app.vo.ResourcePathVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 资源聚合里的收藏夹，其他接口在ResourceAggregationController
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/webInfo")
public class CollectApi {

    @Autowired
    private ResourcePathMapper resourcePathMapper;

    /**
     * 查询收藏
     */
    @GetMapping("/listCollect")
    public ActResult<Map<String, List<ResourcePathVO>>> listCollect() {
        LambdaQueryChainWrapper<ResourcePathPO> wrapper = new LambdaQueryChainWrapper<>(resourcePathMapper);
        List<ResourcePathPO> resourcePathPOS = wrapper.eq(ResourcePathPO::getType, CommonConst.RESOURCE_PATH_TYPE_FAVORITES)
                .eq(ResourcePathPO::getStatus, Boolean.TRUE)
                .orderByAsc(ResourcePathPO::getTitle)
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

package com.siaor.poetize.next.app.api.blog;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.oper.aop.SaveCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.mapper.ResourcePathMapper;
import com.siaor.poetize.next.res.repo.po.ResourcePathPO;
import com.siaor.poetize.next.app.vo.ResourcePathVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 资源聚合里的音乐，其他接口在ResourceAggregationController
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/webInfo")
public class MusicApi {

    @Autowired
    private ResourcePathMapper resourcePathMapper;

    /**
     * 查询音乐
     */
    @GetMapping("/listFunny")
    public ActResult<List<Map<String, Object>>> listFunny() {
        QueryWrapper<ResourcePathPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("classify, count(*) as count")
                .eq("status", Boolean.TRUE)
                .eq("type", CommonConst.RESOURCE_PATH_TYPE_FUNNY)
                .groupBy("classify");
        List<Map<String, Object>> maps = resourcePathMapper.selectMaps(queryWrapper);
        return ActResult.success(maps);
    }

    /**
     * 保存音乐
     */
    @LoginCheck
    @SaveCheck
    @PostMapping("/saveFunny")
    public ActResult saveFunny(@RequestBody ResourcePathVO resourcePathVO) {
        if (!StringUtils.hasText(resourcePathVO.getClassify()) || !StringUtils.hasText(resourcePathVO.getCover()) ||
                !StringUtils.hasText(resourcePathVO.getUrl()) || !StringUtils.hasText(resourcePathVO.getTitle())) {
            return ActResult.fail("信息不全！");
        }
        ResourcePathPO funny = new ResourcePathPO();
        funny.setClassify(resourcePathVO.getClassify());
        funny.setTitle(resourcePathVO.getTitle());
        funny.setCover(resourcePathVO.getCover());
        funny.setUrl(resourcePathVO.getUrl());
        funny.setType(CommonConst.RESOURCE_PATH_TYPE_FUNNY);
        funny.setStatus(Boolean.FALSE);
        resourcePathMapper.insert(funny);
        return ActResult.success();
    }
}

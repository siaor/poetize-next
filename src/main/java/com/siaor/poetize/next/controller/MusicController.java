package com.siaor.poetize.next.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siaor.poetize.next.aop.LoginCheck;
import com.siaor.poetize.next.aop.SaveCheck;
import com.siaor.poetize.next.config.PoetryResult;
import com.siaor.poetize.next.constants.CommonConst;
import com.siaor.poetize.next.dao.ResourcePathMapper;
import com.siaor.poetize.next.entity.ResourcePath;
import com.siaor.poetize.next.vo.ResourcePathVO;
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
public class MusicController {

    @Autowired
    private ResourcePathMapper resourcePathMapper;

    /**
     * 查询音乐
     */
    @GetMapping("/listFunny")
    public PoetryResult<List<Map<String, Object>>> listFunny() {
        QueryWrapper<ResourcePath> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("classify, count(*) as count")
                .eq("status", Boolean.TRUE)
                .eq("type", CommonConst.RESOURCE_PATH_TYPE_FUNNY)
                .groupBy("classify");
        List<Map<String, Object>> maps = resourcePathMapper.selectMaps(queryWrapper);
        return PoetryResult.success(maps);
    }

    /**
     * 保存音乐
     */
    @LoginCheck
    @SaveCheck
    @PostMapping("/saveFunny")
    public PoetryResult saveFunny(@RequestBody ResourcePathVO resourcePathVO) {
        if (!StringUtils.hasText(resourcePathVO.getClassify()) || !StringUtils.hasText(resourcePathVO.getCover()) ||
                !StringUtils.hasText(resourcePathVO.getUrl()) || !StringUtils.hasText(resourcePathVO.getTitle())) {
            return PoetryResult.fail("信息不全！");
        }
        ResourcePath funny = new ResourcePath();
        funny.setClassify(resourcePathVO.getClassify());
        funny.setTitle(resourcePathVO.getTitle());
        funny.setCover(resourcePathVO.getCover());
        funny.setUrl(resourcePathVO.getUrl());
        funny.setType(CommonConst.RESOURCE_PATH_TYPE_FUNNY);
        funny.setStatus(Boolean.FALSE);
        resourcePathMapper.insert(funny);
        return PoetryResult.success();
    }
}

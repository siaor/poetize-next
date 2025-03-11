package com.siaor.poetize.next.app.api.sys;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.repo.po.LabelPO;
import com.siaor.poetize.next.repo.po.SortPO;
import com.siaor.poetize.next.res.aop.LoginCheck;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.repo.mapper.LabelMapper;
import com.siaor.poetize.next.repo.mapper.SortMapper;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.utils.cache.PoetryCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 分类标签 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/webInfo")
public class SortLabelApi {

    @Autowired
    private SortMapper sortMapper;

    @Autowired
    private LabelMapper labelMapper;

    @Autowired
    private CommonQuery commonQuery;

    /**
     * 获取分类标签信息
     */
    @GetMapping("/getSortInfo")
    public PoetryResult<List<SortPO>> getSortInfo() {
        return PoetryResult.success(commonQuery.getSortInfo());
    }

    /**
     * 保存
     */
    @PostMapping("/saveSort")
    @LoginCheck(0)
    public PoetryResult saveSort(@RequestBody SortPO sortPO) {
        if (!StringUtils.hasText(sortPO.getSortName()) || !StringUtils.hasText(sortPO.getSortDescription())) {
            return PoetryResult.fail("分类名称和分类描述不能为空！");
        }

        if (sortPO.getPriority() == null) {
            return PoetryResult.fail("分类必须配置优先级！");
        }

        sortMapper.insert(sortPO);
        PoetryCache.remove(CommonConst.SORT_INFO);
        return PoetryResult.success();
    }


    /**
     * 删除
     */
    @GetMapping("/deleteSort")
    @LoginCheck(0)
    public PoetryResult deleteSort(@RequestParam("id") Integer id) {
        sortMapper.deleteById(id);
        PoetryCache.remove(CommonConst.SORT_INFO);
        return PoetryResult.success();
    }


    /**
     * 更新
     */
    @PostMapping("/updateSort")
    @LoginCheck(0)
    public PoetryResult updateSort(@RequestBody SortPO sortPO) {
        sortMapper.updateById(sortPO);
        PoetryCache.remove(CommonConst.SORT_INFO);
        return PoetryResult.success();
    }


    /**
     * 查询List
     */
    @GetMapping("/listSort")
    public PoetryResult<List<SortPO>> listSort() {
        return PoetryResult.success(new LambdaQueryChainWrapper<>(sortMapper).list());
    }


    /**
     * 保存
     */
    @PostMapping("/saveLabel")
    @LoginCheck(0)
    public PoetryResult saveLabel(@RequestBody LabelPO labelPO) {
        if (!StringUtils.hasText(labelPO.getLabelName()) || !StringUtils.hasText(labelPO.getLabelDescription()) || labelPO.getSortId() == null) {
            return PoetryResult.fail("标签名称和标签描述和分类Id不能为空！");
        }
        labelMapper.insert(labelPO);
        PoetryCache.remove(CommonConst.SORT_INFO);
        return PoetryResult.success();
    }


    /**
     * 删除
     */
    @GetMapping("/deleteLabel")
    @LoginCheck(0)
    public PoetryResult deleteLabel(@RequestParam("id") Integer id) {
        labelMapper.deleteById(id);
        PoetryCache.remove(CommonConst.SORT_INFO);
        return PoetryResult.success();
    }


    /**
     * 更新
     */
    @PostMapping("/updateLabel")
    @LoginCheck(0)
    public PoetryResult updateLabel(@RequestBody LabelPO labelPO) {
        labelMapper.updateById(labelPO);
        PoetryCache.remove(CommonConst.SORT_INFO);
        return PoetryResult.success();
    }


    /**
     * 查询List
     */
    @GetMapping("/listLabel")
    public PoetryResult<List<LabelPO>> listLabel() {
        return PoetryResult.success(new LambdaQueryChainWrapper<>(labelMapper).list());
    }


    /**
     * 查询List
     */
    @GetMapping("/listSortAndLabel")
    public PoetryResult<Map> listSortAndLabel() {
        Map<String, List> map = new HashMap<>();
        map.put("sorts", new LambdaQueryChainWrapper<>(sortMapper).list());
        map.put("labels", new LambdaQueryChainWrapper<>(labelMapper).list());
        return PoetryResult.success(map);
    }
}

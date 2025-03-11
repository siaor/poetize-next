package com.siaor.poetize.next.app.api.sys;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.res.repo.po.LabelPO;
import com.siaor.poetize.next.res.repo.po.SortPO;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.mapper.LabelMapper;
import com.siaor.poetize.next.res.repo.mapper.SortMapper;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.repo.cache.SysCache;
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
    public ActResult<List<SortPO>> getSortInfo() {
        return ActResult.success(commonQuery.getSortInfo());
    }

    /**
     * 保存
     */
    @PostMapping("/saveSort")
    @LoginCheck(0)
    public ActResult saveSort(@RequestBody SortPO sortPO) {
        if (!StringUtils.hasText(sortPO.getSortName()) || !StringUtils.hasText(sortPO.getSortDescription())) {
            return ActResult.fail("分类名称和分类描述不能为空！");
        }

        if (sortPO.getPriority() == null) {
            return ActResult.fail("分类必须配置优先级！");
        }

        sortMapper.insert(sortPO);
        SysCache.remove(CommonConst.SORT_INFO);
        return ActResult.success();
    }


    /**
     * 删除
     */
    @GetMapping("/deleteSort")
    @LoginCheck(0)
    public ActResult deleteSort(@RequestParam("id") Integer id) {
        sortMapper.deleteById(id);
        SysCache.remove(CommonConst.SORT_INFO);
        return ActResult.success();
    }


    /**
     * 更新
     */
    @PostMapping("/updateSort")
    @LoginCheck(0)
    public ActResult updateSort(@RequestBody SortPO sortPO) {
        sortMapper.updateById(sortPO);
        SysCache.remove(CommonConst.SORT_INFO);
        return ActResult.success();
    }


    /**
     * 查询List
     */
    @GetMapping("/listSort")
    public ActResult<List<SortPO>> listSort() {
        return ActResult.success(new LambdaQueryChainWrapper<>(sortMapper).list());
    }


    /**
     * 保存
     */
    @PostMapping("/saveLabel")
    @LoginCheck(0)
    public ActResult saveLabel(@RequestBody LabelPO labelPO) {
        if (!StringUtils.hasText(labelPO.getLabelName()) || !StringUtils.hasText(labelPO.getLabelDescription()) || labelPO.getSortId() == null) {
            return ActResult.fail("标签名称和标签描述和分类Id不能为空！");
        }
        labelMapper.insert(labelPO);
        SysCache.remove(CommonConst.SORT_INFO);
        return ActResult.success();
    }


    /**
     * 删除
     */
    @GetMapping("/deleteLabel")
    @LoginCheck(0)
    public ActResult deleteLabel(@RequestParam("id") Integer id) {
        labelMapper.deleteById(id);
        SysCache.remove(CommonConst.SORT_INFO);
        return ActResult.success();
    }


    /**
     * 更新
     */
    @PostMapping("/updateLabel")
    @LoginCheck(0)
    public ActResult updateLabel(@RequestBody LabelPO labelPO) {
        labelMapper.updateById(labelPO);
        SysCache.remove(CommonConst.SORT_INFO);
        return ActResult.success();
    }


    /**
     * 查询List
     */
    @GetMapping("/listLabel")
    public ActResult<List<LabelPO>> listLabel() {
        return ActResult.success(new LambdaQueryChainWrapper<>(labelMapper).list());
    }


    /**
     * 查询List
     */
    @GetMapping("/listSortAndLabel")
    public ActResult<Map> listSortAndLabel() {
        Map<String, List> map = new HashMap<>();
        map.put("sorts", new LambdaQueryChainWrapper<>(sortMapper).list());
        map.put("labels", new LambdaQueryChainWrapper<>(labelMapper).list());
        return ActResult.success(map);
    }
}

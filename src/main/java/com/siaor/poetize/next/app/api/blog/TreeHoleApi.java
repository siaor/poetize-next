package com.siaor.poetize.next.app.api.blog;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.oper.aop.SaveCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.mapper.TreeHoleMapper;
import com.siaor.poetize.next.res.repo.po.TreeHolePO;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

/**
 * <p>
 * 弹幕 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/webInfo")
public class TreeHoleApi {

    @Autowired
    private TreeHoleMapper treeHoleMapper;

    /**
     * 保存
     */
    @PostMapping("/saveTreeHole")
    @SaveCheck
    public ActResult<TreeHolePO> saveTreeHole(@RequestBody TreeHolePO treeHolePO) {
        if (!StringUtils.hasText(treeHolePO.getMessage())) {
            return ActResult.fail("留言不能为空！");
        }
        treeHoleMapper.insert(treeHolePO);
        if (!StringUtils.hasText(treeHolePO.getAvatar())) {
            treeHolePO.setAvatar(PoetryUtil.getRandomAvatar(null));
        }
        return ActResult.success(treeHolePO);
    }


    /**
     * 删除
     */
    @GetMapping("/deleteTreeHole")
    @LoginCheck(0)
    public ActResult deleteTreeHole(@RequestParam("id") Integer id) {
        treeHoleMapper.deleteById(id);
        return ActResult.success();
    }


    /**
     * 查询List
     */
    @GetMapping("/listTreeHole")
    public ActResult<List<TreeHolePO>> listTreeHole() {
        List<TreeHolePO> treeHoles;
        int count = new LambdaQueryChainWrapper<>(treeHoleMapper).count().intValue();
        if (count > CommonConst.TREE_HOLE_COUNT) {
            int i = new Random().nextInt(count + 1 - CommonConst.TREE_HOLE_COUNT);
            treeHoles = treeHoleMapper.queryAllByLimit(i, CommonConst.TREE_HOLE_COUNT);
        } else {
            treeHoles = new LambdaQueryChainWrapper<>(treeHoleMapper).list();
        }

        treeHoles.forEach(treeHolePO -> {
            if (!StringUtils.hasText(treeHolePO.getAvatar())) {
                treeHolePO.setAvatar(PoetryUtil.getRandomAvatar(treeHolePO.getId().toString()));
            }
        });
        return ActResult.success(treeHoles);
    }
}

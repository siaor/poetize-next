package com.siaor.poetize.next.app.api.blog;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.res.aop.LoginCheck;
import com.siaor.poetize.next.res.aop.SaveCheck;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.repo.mapper.TreeHoleMapper;
import com.siaor.poetize.next.repo.po.TreeHolePO;
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
    public PoetryResult<TreeHolePO> saveTreeHole(@RequestBody TreeHolePO treeHolePO) {
        if (!StringUtils.hasText(treeHolePO.getMessage())) {
            return PoetryResult.fail("留言不能为空！");
        }
        treeHoleMapper.insert(treeHolePO);
        if (!StringUtils.hasText(treeHolePO.getAvatar())) {
            treeHolePO.setAvatar(PoetryUtil.getRandomAvatar(null));
        }
        return PoetryResult.success(treeHolePO);
    }


    /**
     * 删除
     */
    @GetMapping("/deleteTreeHole")
    @LoginCheck(0)
    public PoetryResult deleteTreeHole(@RequestParam("id") Integer id) {
        treeHoleMapper.deleteById(id);
        return PoetryResult.success();
    }


    /**
     * 查询List
     */
    @GetMapping("/listTreeHole")
    public PoetryResult<List<TreeHolePO>> listTreeHole() {
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
        return PoetryResult.success(treeHoles);
    }
}

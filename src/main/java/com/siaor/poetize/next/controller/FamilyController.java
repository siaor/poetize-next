package com.siaor.poetize.next.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.aop.LoginCheck;
import com.siaor.poetize.next.config.PoetryResult;
import com.siaor.poetize.next.entity.Family;
import com.siaor.poetize.next.service.FamilyService;
import com.siaor.poetize.next.constants.CommonConst;
import com.siaor.poetize.next.utils.CommonQuery;
import com.siaor.poetize.next.utils.cache.PoetryCache;
import com.siaor.poetize.next.utils.PoetryUtil;
import com.siaor.poetize.next.vo.BaseRequestVO;
import com.siaor.poetize.next.vo.FamilyVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 家庭信息 前端控制器
 * </p>
 *
 * @author sara
 * @since 2023-01-03
 */
@RestController
@RequestMapping("/family")
public class FamilyController {

    @Autowired
    private FamilyService familyService;

    @Autowired
    private CommonQuery commonQuery;

    /**
     * 保存
     */
    @PostMapping("/saveFamily")
    @LoginCheck
    public PoetryResult saveFamily(@Validated @RequestBody FamilyVO familyVO) {
        Integer userId = PoetryUtil.getUserId();
        familyVO.setUserId(userId);
        Family oldFamily = familyService.lambdaQuery().select(Family::getId).eq(Family::getUserId, userId).one();
        Family family = new Family();
        BeanUtils.copyProperties(familyVO, family);
        if (userId.intValue() == PoetryUtil.getAdminUser().getId().intValue()) {
            family.setStatus(Boolean.TRUE);
        } else {
            family.setStatus(Boolean.FALSE);
        }

        if (oldFamily != null) {
            family.setId(oldFamily.getId());
            familyService.updateById(family);
        } else {
            family.setId(null);
            familyService.save(family);
        }
        if (userId.intValue() == PoetryUtil.getAdminUser().getId().intValue()) {
            PoetryCache.put(CommonConst.ADMIN_FAMILY, family);
        }
        PoetryCache.remove(CommonConst.FAMILY_LIST);
        return PoetryResult.success();
    }

    /**
     * 删除
     */
    @GetMapping("/deleteFamily")
    @LoginCheck(0)
    public PoetryResult deleteFamily(@RequestParam("id") Integer id) {
        familyService.removeById(id);
        PoetryCache.remove(CommonConst.FAMILY_LIST);
        return PoetryResult.success();
    }

    /**
     * 获取
     */
    @GetMapping("/getFamily")
    @LoginCheck
    public PoetryResult<FamilyVO> getFamily() {
        Integer userId = PoetryUtil.getUserId();
        Family family = familyService.lambdaQuery().eq(Family::getUserId, userId).one();
        if (family == null) {
            return PoetryResult.success();
        } else {
            FamilyVO familyVO = new FamilyVO();
            BeanUtils.copyProperties(family, familyVO);
            return PoetryResult.success(familyVO);
        }
    }

    /**
     * 获取
     */
    @GetMapping("/getAdminFamily")
    public PoetryResult<FamilyVO> getAdminFamily() {
        Family family = (Family) PoetryCache.get(CommonConst.ADMIN_FAMILY);
        if (family == null) {
            return PoetryResult.fail("请初始化表白墙");
        }
        FamilyVO familyVO = new FamilyVO();
        BeanUtils.copyProperties(family, familyVO);
        return PoetryResult.success(familyVO);
    }

    /**
     * 查询随机家庭
     */
    @GetMapping("/listRandomFamily")
    public PoetryResult<List<FamilyVO>> listRandomFamily(@RequestParam(value = "size", defaultValue = "10") Integer size) {
        List<FamilyVO> familyList = commonQuery.getFamilyList();
        if (familyList.size() > size) {
            Collections.shuffle(familyList);
            familyList = familyList.subList(0, size);
        }
        return PoetryResult.success(familyList);
    }

    /**
     * 查询
     */
    @PostMapping("/listFamily")
    @LoginCheck(0)
    public PoetryResult<Page> listFamily(@RequestBody BaseRequestVO baseRequestVO) {
        familyService.lambdaQuery()
                .eq(baseRequestVO.getStatus() != null, Family::getStatus, baseRequestVO.getStatus())
                .orderByDesc(Family::getCreateTime).page(baseRequestVO);
        return PoetryResult.success(baseRequestVO);
    }

    /**
     * 修改状态
     */
    @GetMapping("/changeLoveStatus")
    @LoginCheck(0)
    public PoetryResult changeLoveStatus(@RequestParam("id") Integer id, @RequestParam("flag") Boolean flag) {
        familyService.lambdaUpdate().eq(Family::getId, id).set(Family::getStatus, flag).update();
        PoetryCache.remove(CommonConst.FAMILY_LIST);
        return PoetryResult.success();
    }
}

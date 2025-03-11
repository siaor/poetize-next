package com.siaor.poetize.next.app.api.blog;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.repo.po.FamilyPO;
import com.siaor.poetize.next.res.aop.LoginCheck;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.pow.FamilyPow;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.utils.cache.PoetryCache;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import com.siaor.poetize.next.app.vo.FamilyVO;
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
public class FamilyApi {

    @Autowired
    private FamilyPow familyPow;

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
        FamilyPO oldFamilyPO = familyPow.lambdaQuery().select(FamilyPO::getId).eq(FamilyPO::getUserId, userId).one();
        FamilyPO familyPO = new FamilyPO();
        BeanUtils.copyProperties(familyVO, familyPO);
        if (userId.intValue() == PoetryUtil.getAdminUser().getId().intValue()) {
            familyPO.setStatus(Boolean.TRUE);
        } else {
            familyPO.setStatus(Boolean.FALSE);
        }

        if (oldFamilyPO != null) {
            familyPO.setId(oldFamilyPO.getId());
            familyPow.updateById(familyPO);
        } else {
            familyPO.setId(null);
            familyPow.save(familyPO);
        }
        if (userId.intValue() == PoetryUtil.getAdminUser().getId().intValue()) {
            PoetryCache.put(CommonConst.ADMIN_FAMILY, familyPO);
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
        familyPow.removeById(id);
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
        FamilyPO familyPO = familyPow.lambdaQuery().eq(FamilyPO::getUserId, userId).one();
        if (familyPO == null) {
            return PoetryResult.success();
        } else {
            FamilyVO familyVO = new FamilyVO();
            BeanUtils.copyProperties(familyPO, familyVO);
            return PoetryResult.success(familyVO);
        }
    }

    /**
     * 获取
     */
    @GetMapping("/getAdminFamily")
    public PoetryResult<FamilyVO> getAdminFamily() {
        FamilyPO familyPO = (FamilyPO) PoetryCache.get(CommonConst.ADMIN_FAMILY);
        if (familyPO == null) {
            return PoetryResult.fail("请初始化表白墙");
        }
        FamilyVO familyVO = new FamilyVO();
        BeanUtils.copyProperties(familyPO, familyVO);
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
        familyPow.lambdaQuery()
                .eq(baseRequestVO.getStatus() != null, FamilyPO::getStatus, baseRequestVO.getStatus())
                .orderByDesc(FamilyPO::getCreateTime).page(baseRequestVO);
        return PoetryResult.success(baseRequestVO);
    }

    /**
     * 修改状态
     */
    @GetMapping("/changeLoveStatus")
    @LoginCheck(0)
    public PoetryResult changeLoveStatus(@RequestParam("id") Integer id, @RequestParam("flag") Boolean flag) {
        familyPow.lambdaUpdate().eq(FamilyPO::getId, id).set(FamilyPO::getStatus, flag).update();
        PoetryCache.remove(CommonConst.FAMILY_LIST);
        return PoetryResult.success();
    }
}

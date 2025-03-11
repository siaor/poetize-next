package com.siaor.poetize.next.app.api.blog;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.res.aop.LoginCheck;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.siaor.poetize.next.res.aop.SaveCheck;
import com.siaor.poetize.next.repo.mapper.ArticleMapper;
import com.siaor.poetize.next.repo.po.ArticlePO;
import com.siaor.poetize.next.repo.po.WeiYanPO;
import com.siaor.poetize.next.pow.WeiYanPow;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.res.enums.SysEnum;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.res.utils.StringUtil;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * <p>
 * 微言表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-10-26
 */
@RestController
@RequestMapping("/weiYan")
public class WeiYanApi {

    @Autowired
    private WeiYanPow weiYanPow;

    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 保存
     */
    @PostMapping("/saveWeiYan")
    @LoginCheck
    @SaveCheck
    public PoetryResult saveWeiYan(@RequestBody WeiYanPO weiYanPOVO) {
        if (!StringUtils.hasText(weiYanPOVO.getContent())) {
            return PoetryResult.fail("微言不能为空！");
        }

        String content = StringUtil.removeHtml(weiYanPOVO.getContent());
        if (!StringUtils.hasText(content)) {
            return PoetryResult.fail("微言内容不合法！");
        }
        weiYanPOVO.setContent(content);

        WeiYanPO weiYanPO = new WeiYanPO();
        weiYanPO.setUserId(PoetryUtil.getUserId());
        weiYanPO.setContent(weiYanPOVO.getContent());
        weiYanPO.setIsPublic(weiYanPOVO.getIsPublic());
        weiYanPO.setType(CommonConst.WEIYAN_TYPE_FRIEND);
        weiYanPow.save(weiYanPO);
        return PoetryResult.success();
    }


    /**
     * 保存
     */
    @PostMapping("/saveNews")
    @LoginCheck
    public PoetryResult saveNews(@RequestBody WeiYanPO weiYanPOVO) {
        if (!StringUtils.hasText(weiYanPOVO.getContent()) || weiYanPOVO.getSource() == null) {
            return PoetryResult.fail("信息不全！");
        }

        if (weiYanPOVO.getCreateTime() == null) {
            weiYanPOVO.setCreateTime(LocalDateTime.now());
        }

        Integer userId = PoetryUtil.getUserId();

        LambdaQueryChainWrapper<ArticlePO> wrapper = new LambdaQueryChainWrapper<>(articleMapper);
        Integer count = wrapper.eq(ArticlePO::getId, weiYanPOVO.getSource()).eq(ArticlePO::getUserId, userId).count().intValue();

        if (count == null || count < 1) {
            return PoetryResult.fail("来源不存在！");
        }

        WeiYanPO weiYanPO = new WeiYanPO();
        weiYanPO.setUserId(userId);
        weiYanPO.setContent(weiYanPOVO.getContent());
        weiYanPO.setIsPublic(Boolean.TRUE);
        weiYanPO.setSource(weiYanPOVO.getSource());
        weiYanPO.setCreateTime(weiYanPOVO.getCreateTime());
        weiYanPO.setType(CommonConst.WEIYAN_TYPE_NEWS);
        weiYanPow.save(weiYanPO);
        return PoetryResult.success();
    }

    /**
     * 查询List
     */
    @PostMapping("/listNews")
    public PoetryResult<BaseRequestVO> listNews(@RequestBody BaseRequestVO baseRequestVO) {
        if (baseRequestVO.getSource() == null) {
            return PoetryResult.fail("来源不能为空！");
        }
        LambdaQueryChainWrapper<WeiYanPO> lambdaQuery = weiYanPow.lambdaQuery();
        lambdaQuery.eq(WeiYanPO::getType, CommonConst.WEIYAN_TYPE_NEWS);
        lambdaQuery.eq(WeiYanPO::getSource, baseRequestVO.getSource());
        lambdaQuery.eq(WeiYanPO::getIsPublic, SysEnum.PUBLIC.getCode());

        lambdaQuery.orderByDesc(WeiYanPO::getCreateTime).page(baseRequestVO);
        return PoetryResult.success(baseRequestVO);
    }

    /**
     * 删除
     */
    @GetMapping("/deleteWeiYan")
    @LoginCheck
    public PoetryResult deleteWeiYan(@RequestParam("id") Integer id) {
        Integer userId = PoetryUtil.getUserId();
        weiYanPow.lambdaUpdate().eq(WeiYanPO::getId, id)
                .eq(WeiYanPO::getUserId, userId)
                .remove();
        return PoetryResult.success();
    }


    /**
     * 查询List
     */
    @PostMapping("/listWeiYan")
    public PoetryResult<BaseRequestVO> listWeiYan(@RequestBody BaseRequestVO baseRequestVO) {
        LambdaQueryChainWrapper<WeiYanPO> lambdaQuery = weiYanPow.lambdaQuery();
        lambdaQuery.eq(WeiYanPO::getType, CommonConst.WEIYAN_TYPE_FRIEND);
        if (baseRequestVO.getUserId() == null) {
            if (PoetryUtil.getUserId() != null) {
                lambdaQuery.eq(WeiYanPO::getUserId, PoetryUtil.getUserId());
            } else {
                lambdaQuery.eq(WeiYanPO::getIsPublic, SysEnum.PUBLIC.getCode());
                lambdaQuery.eq(WeiYanPO::getUserId, PoetryUtil.getAdminUser().getId());
            }
        } else {
            if (!baseRequestVO.getUserId().equals(PoetryUtil.getUserId())) {
                lambdaQuery.eq(WeiYanPO::getIsPublic, SysEnum.PUBLIC.getCode());
            }
            lambdaQuery.eq(WeiYanPO::getUserId, baseRequestVO.getUserId());
        }

        lambdaQuery.orderByDesc(WeiYanPO::getCreateTime).page(baseRequestVO);
        return PoetryResult.success(baseRequestVO);
    }
}

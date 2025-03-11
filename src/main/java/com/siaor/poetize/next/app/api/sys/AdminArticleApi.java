package com.siaor.poetize.next.app.api.sys;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.res.repo.po.ArticlePO;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.pow.ArticlePow;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.app.vo.ArticleVO;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 后台文章 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/admin")
public class AdminArticleApi {

    @Autowired
    private ArticlePow articlePow;

    /**
     * 用户查询文章
     */
    @PostMapping("/article/user/list")
    @LoginCheck(1)
    public ActResult<Page> listUserArticle(@RequestBody BaseRequestVO baseRequestVO) {
        return articlePow.listAdminArticle(baseRequestVO, false);
    }

    /**
     * Boss查询文章
     */
    @PostMapping("/article/boss/list")
    @LoginCheck(0)
    public ActResult<Page> listBossArticle(@RequestBody BaseRequestVO baseRequestVO) {
        return articlePow.listAdminArticle(baseRequestVO, true);
    }

    /**
     * 修改文章状态
     */
    @GetMapping("/article/changeArticleStatus")
    @LoginCheck(1)
    public ActResult changeArticleStatus(@RequestParam("articleId") Integer articleId,
                                         @RequestParam(value = "viewStatus", required = false) Boolean viewStatus,
                                         @RequestParam(value = "commentStatus", required = false) Boolean commentStatus,
                                         @RequestParam(value = "recommendStatus", required = false) Boolean recommendStatus) {
        LambdaUpdateChainWrapper<ArticlePO> updateChainWrapper = articlePow.lambdaUpdate()
                .eq(ArticlePO::getId, articleId)
                .eq(ArticlePO::getUserId, PoetryUtil.getUserId());
        if (viewStatus != null) {
            updateChainWrapper.set(ArticlePO::getViewStatus, viewStatus);
        }
        if (commentStatus != null) {
            updateChainWrapper.set(ArticlePO::getCommentStatus, commentStatus);
        }
        if (recommendStatus != null) {
            updateChainWrapper.set(ArticlePO::getRecommendStatus, recommendStatus);
        }
        updateChainWrapper.update();
        return ActResult.success();
    }

    /**
     * 查询文章
     */
    @GetMapping("/article/getArticleById")
    @LoginCheck(1)
    public ActResult<ArticleVO> getArticleByIdForUser(@RequestParam("id") Integer id) {
        return articlePow.getArticleByIdForUser(id);
    }
}

package com.siaor.poetize.next.app.api.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.res.repo.po.ArticlePO;
import com.siaor.poetize.next.res.repo.po.CommentPO;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.CommentTypeEnum;
import com.siaor.poetize.next.pow.ArticlePow;
import com.siaor.poetize.next.pow.CommentPow;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 后台评论 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/admin")
public class AdminCommentApi {

    @Autowired
    private ArticlePow articlePow;

    @Autowired
    private CommentPow commentPow;

    /**
     * 作者删除评论
     */
    @GetMapping("/comment/user/deleteComment")
    @LoginCheck(1)
    public ActResult userDeleteComment(@RequestParam("id") Integer id) {
        CommentPO commentPO = commentPow.lambdaQuery().select(CommentPO::getSource, CommentPO::getType).eq(CommentPO::getId, id).one();
        if (commentPO == null) {
            return ActResult.success();
        }
        if (!CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode().equals(commentPO.getType())) {
            return ActResult.fail("权限不足！");
        }
        ArticlePO one = articlePow.lambdaQuery().eq(ArticlePO::getId, commentPO.getSource()).select(ArticlePO::getUserId).one();
        if (one == null || (PoetryUtil.getUserId().intValue() != one.getUserId().intValue())) {
            return ActResult.fail("权限不足！");
        }
        commentPow.removeById(id);
        return ActResult.success();
    }

    /**
     * Boss删除评论
     */
    @GetMapping("/comment/boss/deleteComment")
    @LoginCheck(0)
    public ActResult bossDeleteComment(@RequestParam("id") Integer id) {
        commentPow.removeById(id);
        return ActResult.success();
    }

    /**
     * 用户查询评论
     */
    @PostMapping("/comment/user/list")
    @LoginCheck(1)
    public ActResult<Page> listUserComment(@RequestBody BaseRequestVO baseRequestVO) {
        return commentPow.listAdminComment(baseRequestVO, false);
    }

    /**
     * Boss查询评论
     */
    @PostMapping("/comment/boss/list")
    @LoginCheck(0)
    public ActResult<Page> listBossComment(@RequestBody BaseRequestVO baseRequestVO) {
        return commentPow.listAdminComment(baseRequestVO, true);
    }
}

package com.siaor.poetize.next.app.api.blog;


import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.oper.aop.SaveCheck;
import com.siaor.poetize.next.pow.CommentPow;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.repo.cache.SysCache;
import com.siaor.poetize.next.res.utils.StringUtil;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import com.siaor.poetize.next.app.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 文章评论表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/comment")
public class CommentApi {


    @Autowired
    private CommentPow commentPow;

    @Autowired
    private CommonQuery commonQuery;


    /**
     * 保存评论
     */
    @PostMapping("/saveComment")
    @LoginCheck
    @SaveCheck
    public ActResult saveComment(@Validated @RequestBody CommentVO commentVO) {
        String content = StringUtil.removeHtml(commentVO.getCommentContent());
        if (!StringUtils.hasText(content)) {
            return ActResult.fail("评论内容不合法！");
        }
        commentVO.setCommentContent(content);

        SysCache.remove(CommonConst.COMMENT_COUNT_CACHE + commentVO.getSource().toString() + "_" + commentVO.getType());
        return commentPow.saveComment(commentVO);
    }


    /**
     * 删除评论
     */
    @GetMapping("/deleteComment")
    @LoginCheck
    public ActResult deleteComment(@RequestParam("id") Integer id) {
        return commentPow.deleteComment(id);
    }


    /**
     * 查询评论数量
     */
    @GetMapping("/getCommentCount")
    public ActResult<Integer> getCommentCount(@RequestParam("source") Integer source, @RequestParam("type") String type) {
        return ActResult.success(commonQuery.getCommentCount(source, type));
    }


    /**
     * 查询评论
     */
    @PostMapping("/listComment")
    public ActResult<BaseRequestVO> listComment(@RequestBody BaseRequestVO baseRequestVO) {
        return commentPow.listComment(baseRequestVO);
    }
}


package com.siaor.poetize.next.pro;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.siaor.poetize.next.res.repo.po.ArticlePO;
import com.siaor.poetize.next.res.repo.po.CommentPO;
import com.siaor.poetize.next.res.repo.po.UserPO;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.mapper.ArticleMapper;
import com.siaor.poetize.next.res.repo.mapper.CommentMapper;
import com.siaor.poetize.next.res.norm.ActCode;
import com.siaor.poetize.next.res.norm.CommentTypeEnum;
import com.siaor.poetize.next.pow.CommentPow;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.res.utils.mail.MailSendUtil;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import com.siaor.poetize.next.app.vo.CommentVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 文章评论表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@Service
public class CommentPowPro extends ServiceImpl<CommentMapper, CommentPO> implements CommentPow {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private MailSendUtil mailSendUtil;

    @Override
    public ActResult saveComment(CommentVO commentVO) {
        if (CommentTypeEnum.getEnumByCode(commentVO.getType()) == null) {
            return ActResult.fail("评论来源类型不存在！");
        }
        ArticlePO one = null;
        if (CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode().equals(commentVO.getType())) {
            LambdaQueryChainWrapper<ArticlePO> articleWrapper = new LambdaQueryChainWrapper<>(articleMapper);
            one = articleWrapper.eq(ArticlePO::getId, commentVO.getSource()).select(ArticlePO::getUserId, ArticlePO::getArticleTitle, ArticlePO::getCommentStatus).one();

            if (one == null) {
                return ActResult.fail("文章不存在");
            } else {
                if (!one.getCommentStatus()) {
                    return ActResult.fail("评论功能已关闭！");
                }
            }
        }


        CommentPO commentPO = new CommentPO();
        commentPO.setSource(commentVO.getSource());
        commentPO.setType(commentVO.getType());
        commentPO.setCommentContent(commentVO.getCommentContent());
        commentPO.setParentCommentId(commentVO.getParentCommentId());
        commentPO.setFloorCommentId(commentVO.getFloorCommentId());
        commentPO.setParentUserId(commentVO.getParentUserId());
        commentPO.setUserId(PoetryUtil.getUserId());
        if (StringUtils.hasText(commentVO.getCommentInfo())) {
            commentPO.setCommentInfo(commentVO.getCommentInfo());
        }
        save(commentPO);

        try {
            mailSendUtil.sendCommentMail(commentVO, one, this);
        } catch (Exception e) {
            log.error("发送评论邮件失败：", e);
        }

        return ActResult.success();
    }

    @Override
    public ActResult deleteComment(Integer id) {
        Integer userId = PoetryUtil.getUserId();
        lambdaUpdate().eq(CommentPO::getId, id)
                .eq(CommentPO::getUserId, userId)
                .remove();
        return ActResult.success();
    }

    @Override
    public ActResult<BaseRequestVO> listComment(BaseRequestVO baseRequestVO) {
        if (baseRequestVO.getSource() == null || !StringUtils.hasText(baseRequestVO.getCommentType())) {
            return ActResult.fail(ActCode.PARAMETER_ERROR);
        }

        if (CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode().equals(baseRequestVO.getCommentType())) {
            LambdaQueryChainWrapper<ArticlePO> articleWrapper = new LambdaQueryChainWrapper<>(articleMapper);
            ArticlePO one = articleWrapper.eq(ArticlePO::getId, baseRequestVO.getSource()).select(ArticlePO::getCommentStatus).one();

            if (one != null && !one.getCommentStatus()) {
                return ActResult.fail("评论功能已关闭！");
            }
        }


        if (baseRequestVO.getFloorCommentId() == null) {
            lambdaQuery().eq(CommentPO::getSource, baseRequestVO.getSource()).eq(CommentPO::getType, baseRequestVO.getCommentType()).eq(CommentPO::getParentCommentId, CommonConst.FIRST_COMMENT).orderByDesc(CommentPO::getCreateTime).page(baseRequestVO);
            List<CommentPO> commentPOS = baseRequestVO.getRecords();
            if (CollectionUtils.isEmpty(commentPOS)) {
                return ActResult.success(baseRequestVO);
            }
            List<CommentVO> commentVOs = commentPOS.stream().map(c -> {
                CommentVO commentVO = buildCommentVO(c);
                Page page = new Page(1, 5);
                lambdaQuery().eq(CommentPO::getSource, baseRequestVO.getSource()).eq(CommentPO::getType, baseRequestVO.getCommentType()).eq(CommentPO::getFloorCommentId, c.getId()).orderByAsc(CommentPO::getCreateTime).page(page);
                List<CommentPO> childCommentPOS = page.getRecords();
                if (childCommentPOS != null) {
                    List<CommentVO> ccVO = childCommentPOS.stream().map(cc -> buildCommentVO(cc)).collect(Collectors.toList());
                    page.setRecords(ccVO);
                }
                commentVO.setChildComments(page);
                return commentVO;
            }).collect(Collectors.toList());
            baseRequestVO.setRecords(commentVOs);
        } else {
            lambdaQuery().eq(CommentPO::getSource, baseRequestVO.getSource()).eq(CommentPO::getType, baseRequestVO.getCommentType()).eq(CommentPO::getFloorCommentId, baseRequestVO.getFloorCommentId()).orderByAsc(CommentPO::getCreateTime).page(baseRequestVO);
            List<CommentPO> childCommentPOS = baseRequestVO.getRecords();
            if (CollectionUtils.isEmpty(childCommentPOS)) {
                return ActResult.success(baseRequestVO);
            }
            List<CommentVO> ccVO = childCommentPOS.stream().map(cc -> buildCommentVO(cc)).collect(Collectors.toList());
            baseRequestVO.setRecords(ccVO);
        }
        return ActResult.success(baseRequestVO);
    }

    @Override
    public ActResult<Page> listAdminComment(BaseRequestVO baseRequestVO, Boolean isBoss) {
        LambdaQueryChainWrapper<CommentPO> wrapper = lambdaQuery();
        if (isBoss) {
            if (baseRequestVO.getSource() != null) {
                wrapper.eq(CommentPO::getSource, baseRequestVO.getSource());
            }
            if (StringUtils.hasText(baseRequestVO.getCommentType())) {
                wrapper.eq(CommentPO::getType, baseRequestVO.getCommentType());
            }
            wrapper.orderByDesc(CommentPO::getCreateTime).page(baseRequestVO);
        } else {
            List<Integer> userArticleIds = commonQuery.getUserArticleIds(PoetryUtil.getUserId());
            if (CollectionUtils.isEmpty(userArticleIds)) {
                baseRequestVO.setTotal(0);
                baseRequestVO.setRecords(new ArrayList());
            } else {
                if (baseRequestVO.getSource() != null) {
                    wrapper.eq(CommentPO::getSource, baseRequestVO.getSource()).eq(CommentPO::getType, CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode());
                } else {
                    wrapper.eq(CommentPO::getType, CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode()).in(CommentPO::getSource, userArticleIds);
                }
                wrapper.orderByDesc(CommentPO::getCreateTime).page(baseRequestVO);
            }
        }
        return ActResult.success(baseRequestVO);
    }

    private CommentVO buildCommentVO(CommentPO c) {
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(c, commentVO);

        UserPO userPO = commonQuery.getUser(commentVO.getUserId());
        if (userPO != null) {
            commentVO.setAvatar(userPO.getAvatar());
            commentVO.setUsername(userPO.getUsername());
        }

        if (!StringUtils.hasText(commentVO.getUsername())) {
            commentVO.setUsername(PoetryUtil.getRandomName(commentVO.getUserId().toString()));
        }

        if (commentVO.getParentUserId() != null) {
            UserPO u = commonQuery.getUser(commentVO.getParentUserId());
            if (u != null) {
                commentVO.setParentUsername(u.getUsername());
            }
            if (!StringUtils.hasText(commentVO.getParentUsername())) {
                commentVO.setParentUsername(PoetryUtil.getRandomName(commentVO.getParentUserId().toString()));
            }
        }
        return commentVO;
    }
}

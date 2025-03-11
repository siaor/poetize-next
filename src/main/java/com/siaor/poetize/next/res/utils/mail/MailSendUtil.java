package com.siaor.poetize.next.res.utils.mail;

import com.siaor.poetize.next.repo.po.CommentPO;
import com.siaor.poetize.next.repo.po.UserPO;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.repo.po.ArticlePO;
import com.siaor.poetize.next.repo.po.WebInfoPO;
import com.siaor.poetize.next.res.enums.CommentTypeEnum;
import com.siaor.poetize.next.repo.po.im.ChatUserMessagePO;
import com.siaor.poetize.next.pow.CommentPow;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.res.utils.cache.PoetryCache;
import com.siaor.poetize.next.app.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MailSendUtil {

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private MailUtil mailUtil;

    public void sendCommentMail(CommentVO commentVO, ArticlePO one, CommentPow commentPow) {
        List<String> mail = new ArrayList<>();
        String toName = "";
        if (commentVO.getParentUserId() != null) {
            UserPO userPO = commonQuery.getUser(commentVO.getParentUserId());
            if (userPO != null && !userPO.getId().equals(PoetryUtil.getUserId()) && StringUtils.hasText(userPO.getEmail())) {
                toName = userPO.getUsername();
                mail.add(userPO.getEmail());
            }
        } else {
            if (CommentTypeEnum.COMMENT_TYPE_MESSAGE.getCode().equals(commentVO.getType()) ||
                    CommentTypeEnum.COMMENT_TYPE_LOVE.getCode().equals(commentVO.getType())) {
                UserPO adminUserPO = PoetryUtil.getAdminUser();
                if (StringUtils.hasText(adminUserPO.getEmail()) && !Objects.equals(PoetryUtil.getUserId(), adminUserPO.getId())) {
                    mail.add(adminUserPO.getEmail());
                }
            } else if (CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode().equals(commentVO.getType())) {
                UserPO userPO = commonQuery.getUser(one.getUserId());
                if (userPO != null && StringUtils.hasText(userPO.getEmail()) && !userPO.getId().equals(PoetryUtil.getUserId())) {
                    mail.add(userPO.getEmail());
                }
            }
        }

        if (!CollectionUtils.isEmpty(mail)) {
            String sourceName = "";
            if (CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode().equals(commentVO.getType())) {
                sourceName = one.getArticleTitle();
            }
            String commentMail = getCommentMail(commentVO.getType(), sourceName,
                    PoetryUtil.getUsername(),
                    commentVO.getCommentContent(),
                    toName,
                    commentVO.getParentCommentId(), commentPow);

            AtomicInteger count = (AtomicInteger) PoetryCache.get(CommonConst.COMMENT_IM_MAIL + mail.get(0));
            if (count == null || count.get() < CommonConst.COMMENT_IM_MAIL_COUNT) {
                WebInfoPO webInfoPO = (WebInfoPO) PoetryCache.get(CommonConst.WEB_INFO);
                mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfoPO == null ? "POETIZE-NEXT" : webInfoPO.getWebName()) + "的回执！", commentMail);
                if (count == null) {
                    PoetryCache.put(CommonConst.COMMENT_IM_MAIL + mail.get(0), new AtomicInteger(1), CommonConst.CODE_EXPIRE);
                } else {
                    count.incrementAndGet();
                }
            }
        }
    }

    /**
     * source：0留言 其他是文章标题
     * fromName：评论人
     * toName：被评论人
     */
    private String getCommentMail(String commentType, String source, String fromName, String fromContent, String toName, Integer toCommentId, CommentPow commentPow) {
        WebInfoPO webInfoPO = (WebInfoPO) PoetryCache.get(CommonConst.WEB_INFO);
        String webName = (webInfoPO == null ? "POETIZE-NEXT" : webInfoPO.getWebName());

        String mailType = "";
        String toMail = "";
        if (StringUtils.hasText(toName)) {
            mailType = String.format(MailUtil.replyMail, fromName);
            CommentPO toCommentPO = commentPow.lambdaQuery().select(CommentPO::getCommentContent).eq(CommentPO::getId, toCommentId).one();
            if (toCommentPO != null) {
                toMail = String.format(MailUtil.originalText, toName, toCommentPO.getCommentContent());
            }
        } else {
            if (CommentTypeEnum.COMMENT_TYPE_MESSAGE.getCode().equals(commentType)) {
                mailType = String.format(MailUtil.messageMail, fromName);
            } else if (CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode().equals(commentType)) {
                mailType = String.format(MailUtil.commentMail, source, fromName);
            } else if (CommentTypeEnum.COMMENT_TYPE_LOVE.getCode().equals(commentType)) {
                mailType = String.format(MailUtil.loveMail, fromName);
            }
        }

        return String.format(mailUtil.getMailText(),
                webName,
                mailType,
                fromName,
                fromContent,
                toMail,
                webName);
    }

    public void sendImMail(ChatUserMessagePO message) {
        if (!message.getMessageStatus()) {
            List<String> mail = new ArrayList<>();
            String username = "";
            UserPO toUserPO = commonQuery.getUser(message.getToId());
            if (toUserPO != null && StringUtils.hasText(toUserPO.getEmail())) {
                mail.add(toUserPO.getEmail());
            }
            UserPO fromUserPO = commonQuery.getUser(message.getFromId());
            if (fromUserPO != null) {
                username = fromUserPO.getUsername();
            }

            if (!CollectionUtils.isEmpty(mail)) {
                String commentMail = getImMail(username, message.getContent());

                AtomicInteger count = (AtomicInteger) PoetryCache.get(CommonConst.COMMENT_IM_MAIL + mail.get(0));
                if (count == null || count.get() < CommonConst.COMMENT_IM_MAIL_COUNT) {
                    WebInfoPO webInfoPO = (WebInfoPO) PoetryCache.get(CommonConst.WEB_INFO);
                    mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfoPO == null ? "POETIZE-NEXT" : webInfoPO.getWebName()) + "的回执！", commentMail);
                    if (count == null) {
                        PoetryCache.put(CommonConst.COMMENT_IM_MAIL + mail.get(0), new AtomicInteger(1), CommonConst.CODE_EXPIRE);
                    } else {
                        count.incrementAndGet();
                    }
                }
            }
        }
    }

    private String getImMail(String fromName, String fromContent) {
        WebInfoPO webInfoPO = (WebInfoPO) PoetryCache.get(CommonConst.WEB_INFO);
        String webName = (webInfoPO == null ? "POETIZE-NEXT" : webInfoPO.getWebName());

        return String.format(mailUtil.getMailText(),
                webName,
                String.format(MailUtil.imMail, fromName),
                fromName,
                fromContent,
                "",
                webName);
    }
}

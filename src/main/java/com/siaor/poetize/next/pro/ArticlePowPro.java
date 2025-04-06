package com.siaor.poetize.next.pro;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.siaor.poetize.next.app.vo.ArticleVO;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import com.siaor.poetize.next.pow.ArticlePow;
import com.siaor.poetize.next.pow.UserPow;
import com.siaor.poetize.next.res.norm.*;
import com.siaor.poetize.next.res.oper.aop.ResourceCheck;
import com.siaor.poetize.next.res.repo.cache.SysCache;
import com.siaor.poetize.next.res.repo.mapper.ArticleMapper;
import com.siaor.poetize.next.res.repo.mapper.LabelMapper;
import com.siaor.poetize.next.res.repo.mapper.PayOrderMapper;
import com.siaor.poetize.next.res.repo.mapper.SortMapper;
import com.siaor.poetize.next.res.repo.po.*;
import com.siaor.poetize.next.res.task.ArticleScanTask;
import com.siaor.poetize.next.res.utils.CommonQuery;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.res.utils.mail.MailUtil;
import com.siaor.poetize.next.res.utils.storage.ArticleFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 文章表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@Service
@Slf4j
public class ArticlePowPro extends ServiceImpl<ArticleMapper, ArticlePO> implements ArticlePow {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private UserPow userPow;

    @Autowired
    private MailUtil mailUtil;

    @Autowired
    private SortMapper sortMapper;

    @Autowired
    private LabelMapper labelMapper;

    @Value("${user.subscribe.format}")
    private String subscribeFormat;

    @Autowired
    private ArticleFileUtil articleFileUtil;

    @Autowired
    private ArticleScanTask articleScanTask;

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Override
    public ActResult saveArticle(ArticleVO articleVO) {
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && !StringUtils.hasText(articleVO.getPassword())) {
            return ActResult.fail("请设置文章密码！");
        }
        ArticlePO articlePO = new ArticlePO();
        if (StringUtils.hasText(articleVO.getArticleCover())) {
            articlePO.setArticleCover(articleVO.getArticleCover());
        }
        if (StringUtils.hasText(articleVO.getVideoUrl())) {
            articlePO.setVideoUrl(articleVO.getVideoUrl());
        }
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && StringUtils.hasText(articleVO.getPassword())) {
            articlePO.setPassword(articleVO.getPassword());
            articlePO.setTips(articleVO.getTips());
            articlePO.setMoney(articleVO.getMoney());
        }
        articlePO.setViewStatus(articleVO.getViewStatus());
        articlePO.setCommentStatus(articleVO.getCommentStatus());
        articlePO.setRecommendStatus(articleVO.getRecommendStatus());
        articlePO.setArticleTitle(articleVO.getArticleTitle());
        articlePO.setArticleIntro(articleVO.getArticleIntro());
        articlePO.setArticleContent(articleVO.getArticleContent());
        articlePO.setSortId(articleVO.getSortId());
        articlePO.setLabelId(articleVO.getLabelId());
        articlePO.setUserId(PoetryUtil.getUserId());
        save(articlePO);

        SysCache.remove(CommonConst.SORT_INFO);

        try {
            if (articleVO.getViewStatus()) {
                List<UserPO> userPOS = userPow.lambdaQuery().select(UserPO::getEmail, UserPO::getSubscribe).eq(UserPO::getUserStatus, SysEnum.STATUS_ENABLE.getCode()).list();
                List<String> emails = userPOS.stream().filter(u -> {
                    List<Integer> sub = JSON.parseArray(u.getSubscribe(), Integer.class);
                    return !CollectionUtils.isEmpty(sub) && sub.contains(articleVO.getLabelId());
                }).map(UserPO::getEmail).collect(Collectors.toList());

                if (!CollectionUtils.isEmpty(emails)) {
                    LambdaQueryChainWrapper<LabelPO> wrapper = new LambdaQueryChainWrapper<>(labelMapper);
                    LabelPO labelPO = wrapper.select(LabelPO::getLabelName).eq(LabelPO::getId, articleVO.getLabelId()).one();
                    String text = getSubscribeMail(labelPO.getLabelName(), articleVO.getArticleTitle());
                    WebInfoPO webInfoPO = (WebInfoPO) SysCache.get(CommonConst.WEB_INFO);
                    mailUtil.sendMailMessage(emails, "您有一封来自" + (webInfoPO == null ? "POETIZE" : webInfoPO.getWebName()) + "的回执！", text);
                }
            }
        } catch (Exception e) {
            log.error("订阅邮件发送失败：", e);
        }

        //创建文章md文件
        articleFileUtil.create(articlePO);

        return ActResult.success();
    }

    private String getSubscribeMail(String labelName, String articleTitle) {
        WebInfoPO webInfoPO = (WebInfoPO) SysCache.get(CommonConst.WEB_INFO);
        String webName = (webInfoPO == null ? "POETIZE" : webInfoPO.getWebName());
        return String.format(mailUtil.getMailText(),
                webName,
                String.format(MailUtil.notificationMail, PoetryUtil.getAdminUser().getUsername()),
                PoetryUtil.getAdminUser().getUsername(),
                String.format(subscribeFormat, labelName, articleTitle),
                "",
                webName);
    }

    @Override
    public ActResult deleteArticle(Integer id) {
        Integer userId = PoetryUtil.getUserId();
        lambdaUpdate().eq(ArticlePO::getId, id)
                .eq(ArticlePO::getUserId, userId)
                .remove();
        SysCache.remove(CommonConst.SORT_INFO);
        articleFileUtil.delete(userId, id);
        return ActResult.success();
    }

    @Override
    public ActResult updateArticle(ArticleVO articleVO) {
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && !StringUtils.hasText(articleVO.getPassword())) {
            return ActResult.fail("请设置文章密码！");
        }

        Integer userId = PoetryUtil.getUserId();
        LambdaUpdateChainWrapper<ArticlePO> updateChainWrapper = lambdaUpdate()
                .eq(ArticlePO::getId, articleVO.getId())
                .eq(ArticlePO::getUserId, userId)
                .set(ArticlePO::getLabelId, articleVO.getLabelId())
                .set(ArticlePO::getSortId, articleVO.getSortId())
                .set(ArticlePO::getArticleTitle, articleVO.getArticleTitle())
                .set(ArticlePO::getArticleIntro, articleVO.getArticleIntro())
                .set(ArticlePO::getUpdateBy, PoetryUtil.getUsername())
                .set(ArticlePO::getUpdateTime, LocalDateTime.now())
                .set(ArticlePO::getVideoUrl, StringUtils.hasText(articleVO.getVideoUrl()) ? articleVO.getVideoUrl() : null)
                .set(ArticlePO::getArticleContent, articleVO.getArticleContent());

        if (StringUtils.hasText(articleVO.getArticleCover())) {
            updateChainWrapper.set(ArticlePO::getArticleCover, articleVO.getArticleCover());
        }
        if (articleVO.getCommentStatus() != null) {
            updateChainWrapper.set(ArticlePO::getCommentStatus, articleVO.getCommentStatus());
        }
        if (articleVO.getRecommendStatus() != null) {
            updateChainWrapper.set(ArticlePO::getRecommendStatus, articleVO.getRecommendStatus());
        }
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && StringUtils.hasText(articleVO.getPassword())) {
            updateChainWrapper.set(ArticlePO::getPassword, articleVO.getPassword());
            updateChainWrapper.set(StringUtils.hasText(articleVO.getTips()), ArticlePO::getTips, articleVO.getTips());
            updateChainWrapper.set(ArticlePO::getMoney, articleVO.getMoney());
        }
        if (articleVO.getViewStatus() != null) {
            updateChainWrapper.set(ArticlePO::getViewStatus, articleVO.getViewStatus());
        }
        updateChainWrapper.update();
        SysCache.remove(CommonConst.SORT_INFO);

        ArticlePO articlePO = new ArticlePO();
        BeanUtils.copyProperties(articleVO, articlePO);
        articleFileUtil.update(articlePO);
        return ActResult.success();
    }

    @Override
    public ActResult reload() {
        Integer userId = PoetryUtil.getUserId();
        //获取全部文章文件信息
        List<String> fileList = articleFileUtil.list(userId);
        if (fileList.isEmpty()) {
            return ActResult.success();
        }

        //加载到任务列表
        articleScanTask.put(fileList);

        //启动重载任务
        taskHandle(userId);

        return ActResult.success("正在为您重载文章，数量：" + fileList.size());
    }

    private void taskHandle(Integer userId) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (articleScanTask.isEmpty()) {
                log.info("文章已全部重载完毕！");
                scheduler.shutdownNow();
            } else {
                try {
                    String filePath = articleScanTask.take();
                    log.info("正在载入文章：{}", filePath);
                    ArticlePO articlePO = articleFileUtil.read(filePath);
                    if (articlePO == null) {
                        return;
                    }

                    Integer articleId = articlePO.getId();
                    if (articleId != null) {
                        log.info("文章已存在，跳过载入！");
                        return;
                    }
                    //补全数据
                    articlePO.setUserId(userId);

                    save(articlePO);
                    articleFileUtil.appendId(filePath, articlePO.getId());
                    log.info("文章载入成功！");
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public ActResult<Page> listArticle(BaseRequestVO baseRequestVO) {
        List<Integer> ids = null;
        List<List<Integer>> idList = null;
        if (StringUtils.hasText(baseRequestVO.getArticleSearch())) {
            idList = commonQuery.getArticleIds(baseRequestVO.getArticleSearch());
            ids = idList.stream().flatMap(Collection::stream).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(ids)) {
                baseRequestVO.setRecords(new ArrayList<>());
                return ActResult.success(baseRequestVO);
            }
        }

        LambdaQueryChainWrapper<ArticlePO> lambdaQuery = lambdaQuery();
        lambdaQuery.in(!CollectionUtils.isEmpty(ids), ArticlePO::getId, ids);
        lambdaQuery.like(StringUtils.hasText(baseRequestVO.getSearchKey()), ArticlePO::getArticleTitle, baseRequestVO.getSearchKey());
        lambdaQuery.eq(baseRequestVO.getRecommendStatus() != null && baseRequestVO.getRecommendStatus(), ArticlePO::getRecommendStatus, SysEnum.STATUS_ENABLE.getCode());


        if (baseRequestVO.getLabelId() != null) {
            lambdaQuery.eq(ArticlePO::getLabelId, baseRequestVO.getLabelId());
        } else if (baseRequestVO.getSortId() != null) {
            lambdaQuery.eq(ArticlePO::getSortId, baseRequestVO.getSortId());
        }

        lambdaQuery.orderByDesc(ArticlePO::getCreateTime);

        lambdaQuery.page(baseRequestVO);

        List<ArticlePO> records = baseRequestVO.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            List<ArticleVO> articles = new ArrayList<>();
            List<ArticleVO> titles = new ArrayList<>();
            List<ArticleVO> contents = new ArrayList<>();

            for (ArticlePO articlePO : records) {
                if (articlePO.getArticleContent().length() > CommonConst.SUMMARY) {
                    articlePO.setArticleContent(articlePO.getArticleContent().substring(0, CommonConst.SUMMARY).replace("`", "").replace("#", "").replace(">", ""));
                }
                ArticleVO articleVO = buildArticleVO(articlePO, false);
                articleVO.setHasVideo(StringUtils.hasText(articleVO.getVideoUrl()));
                articleVO.setPassword(null);
                articleVO.setVideoUrl(null);
                if (CollectionUtils.isEmpty(ids)) {
                    articles.add(articleVO);
                } else if (idList.get(0).contains(articleVO.getId())) {
                    titles.add(articleVO);
                } else if (idList.get(1).contains(articleVO.getId())) {
                    contents.add(articleVO);
                }
            }

            List<ArticleVO> collect = new ArrayList<>();
            collect.addAll(articles);
            collect.addAll(titles);
            collect.addAll(contents);
            baseRequestVO.setRecords(collect);
        }
        return ActResult.success(baseRequestVO);
    }

    @Override
    @ResourceCheck(CommonConst.RESOURCE_ARTICLE_DOC)
    public ActResult<ArticleVO> getArticleById(Integer id, String password) {
        LambdaQueryChainWrapper<ArticlePO> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(ArticlePO::getId, id);

        ArticlePO articlePO = lambdaQuery.one();
        if (articlePO == null) {
            return ActResult.fail(ActCode.RES_LOSE);
        }

        boolean needCheck = !articlePO.getViewStatus();
        ArticleVO articleVO;
        if (needCheck) {
            //用户是否已经购买
            Integer userId = PoetryUtil.getUserId();
            if (userId != null) {
                QueryWrapper<PayOrderPO> payQW = new QueryWrapper<>();
                payQW.eq("act_type", PayOrderActType.ARTICLE)
                        .eq("status", PayOrderStatus.PAID)
                        .eq("user_id", userId)
                        .eq("act_id", id);
                if (payOrderMapper.exists(payQW)) {
                    needCheck = false;
                }
            }
            if (needCheck) {
                if (!StringUtils.hasText(password)) {
                    articleVO = new ArticleVO();
                    articleVO.setId(articlePO.getId());
                    articleVO.setTips(articlePO.getTips());
                    articleVO.setMoney(articlePO.getMoney());
                    return ActResult.fail(ActCode.PWD_NEED, articleVO);
                }
                if (!articlePO.getPassword().equals(password)) {
                    return ActResult.fail(ActCode.PWD_ERROR);
                }
            }
        }

        articleMapper.updateViewCount(id);
        articlePO.setPassword(null);
        if (StringUtils.hasText(articlePO.getVideoUrl())) {
            articlePO.setVideoUrl(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).encryptBase64(articlePO.getVideoUrl()));
        }
        articleVO = buildArticleVO(articlePO, false);
        return ActResult.success(articleVO);
    }

    @Override
    public ActResult<Page> listAdminArticle(BaseRequestVO baseRequestVO, Boolean isBoss) {
        LambdaQueryChainWrapper<ArticlePO> lambdaQuery = lambdaQuery();
        lambdaQuery.select(ArticlePO.class, a -> !a.getColumn().equals("article_content"));
        if (!isBoss) {
            lambdaQuery.eq(ArticlePO::getUserId, PoetryUtil.getUserId());
        } else {
            if (baseRequestVO.getUserId() != null) {
                lambdaQuery.eq(ArticlePO::getUserId, baseRequestVO.getUserId());
            }
        }
        if (StringUtils.hasText(baseRequestVO.getSearchKey())) {
            lambdaQuery.like(ArticlePO::getArticleTitle, baseRequestVO.getSearchKey());
        }
        if (baseRequestVO.getRecommendStatus() != null && baseRequestVO.getRecommendStatus()) {
            lambdaQuery.eq(ArticlePO::getRecommendStatus, SysEnum.STATUS_ENABLE.getCode());
        }

        if (baseRequestVO.getLabelId() != null) {
            lambdaQuery.eq(ArticlePO::getLabelId, baseRequestVO.getLabelId());
        }

        if (baseRequestVO.getSortId() != null) {
            lambdaQuery.eq(ArticlePO::getSortId, baseRequestVO.getSortId());
        }

        lambdaQuery.orderByDesc(ArticlePO::getCreateTime).page(baseRequestVO);

        List<ArticlePO> records = baseRequestVO.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            List<ArticleVO> collect = records.stream().map(article -> {
                article.setPassword(null);
                ArticleVO articleVO = buildArticleVO(article, true);
                return articleVO;
            }).collect(Collectors.toList());
            baseRequestVO.setRecords(collect);
        }
        return ActResult.success(baseRequestVO);
    }

    @Override
    public ActResult<ArticleVO> getArticleByIdForUser(Integer id) {
        LambdaQueryChainWrapper<ArticlePO> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(ArticlePO::getId, id).eq(ArticlePO::getUserId, PoetryUtil.getUserId());
        ArticlePO articlePO = lambdaQuery.one();
        if (articlePO == null) {
            return ActResult.fail("文章不存在！");
        }
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(articlePO, articleVO);
        return ActResult.success(articleVO);
    }

    @Override
    public ActResult<Map<Integer, List<ArticleVO>>> listSortArticle() {
        Map<Integer, List<ArticleVO>> result = (Map<Integer, List<ArticleVO>>) SysCache.get(CommonConst.SORT_ARTICLE_LIST);
        if (result != null) {
            return ActResult.success(result);
        }

        synchronized (CommonConst.SORT_ARTICLE_LIST.intern()) {
            result = (Map<Integer, List<ArticleVO>>) SysCache.get(CommonConst.SORT_ARTICLE_LIST);
            if (result == null) {
                Map<Integer, List<ArticleVO>> map = new HashMap<>();

                List<SortPO> sortPOS = new LambdaQueryChainWrapper<>(sortMapper).select(SortPO::getId).list();
                for (SortPO sortPO : sortPOS) {
                    LambdaQueryChainWrapper<ArticlePO> lambdaQuery = lambdaQuery()
                            .eq(ArticlePO::getSortId, sortPO.getId())
                            .orderByDesc(ArticlePO::getCreateTime)
                            .last("limit 6");
                    List<ArticlePO> articlePOList = lambdaQuery.list();
                    if (CollectionUtils.isEmpty(articlePOList)) {
                        continue;
                    }

                    List<ArticleVO> articleVOList = articlePOList.stream().map(article -> {
                        if (article.getArticleContent().length() > CommonConst.SUMMARY) {
                            article.setArticleContent(article.getArticleContent().substring(0, CommonConst.SUMMARY).replace("`", "").replace("#", "").replace(">", ""));
                        }

                        ArticleVO vo = buildArticleVO(article, false);
                        vo.setHasVideo(StringUtils.hasText(article.getVideoUrl()));
                        vo.setPassword(null);
                        vo.setVideoUrl(null);
                        return vo;
                    }).collect(Collectors.toList());
                    map.put(sortPO.getId(), articleVOList);
                }

                SysCache.put(CommonConst.SORT_ARTICLE_LIST, map, CommonConst.TOKEN_INTERVAL);
                return ActResult.success(map);
            } else {
                return ActResult.success(result);
            }
        }
    }

    private ArticleVO buildArticleVO(ArticlePO articlePO, Boolean isAdmin) {
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(articlePO, articleVO);
        if (!isAdmin) {
            if (!StringUtils.hasText(articleVO.getArticleCover())) {
                articleVO.setArticleCover(PoetryUtil.getRandomCover(articleVO.getId().toString()));
            }
        }

        UserPO userPO = commonQuery.getUser(articleVO.getUserId());
        if (userPO != null && StringUtils.hasText(userPO.getUsername())) {
            articleVO.setUsername(userPO.getUsername());
        } else if (!isAdmin) {
            articleVO.setUsername(PoetryUtil.getRandomName(articleVO.getUserId().toString()));
        }
        if (articleVO.getCommentStatus()) {
            articleVO.setCommentCount(commonQuery.getCommentCount(articleVO.getId(), CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode()));
        } else {
            articleVO.setCommentCount(0);
        }

        List<SortPO> sortPOInfo = commonQuery.getSortInfo();
        if (!CollectionUtils.isEmpty(sortPOInfo)) {
            for (SortPO s : sortPOInfo) {
                if (s.getId().intValue() == articleVO.getSortId().intValue()) {
                    SortPO sortPO = new SortPO();
                    BeanUtils.copyProperties(s, sortPO);
                    sortPO.setLabels(null);
                    articleVO.setSort(sortPO);
                    if (!CollectionUtils.isEmpty(s.getLabels())) {
                        for (int j = 0; j < s.getLabels().size(); j++) {
                            LabelPO l = s.getLabels().get(j);
                            if (l.getId().intValue() == articleVO.getLabelId().intValue()) {
                                LabelPO labelPO = new LabelPO();
                                BeanUtils.copyProperties(l, labelPO);
                                articleVO.setLabel(labelPO);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        return articleVO;
    }
}

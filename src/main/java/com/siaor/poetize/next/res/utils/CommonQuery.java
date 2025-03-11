package com.siaor.poetize.next.res.utils;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.res.constants.CommonConst;
import com.siaor.poetize.next.pow.UserPow;
import com.siaor.poetize.next.repo.mapper.*;
import com.siaor.poetize.next.repo.po.*;
import com.siaor.poetize.next.res.utils.cache.PoetryCache;
import com.siaor.poetize.next.app.vo.FamilyVO;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;


@Component
public class CommonQuery {
    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private HistoryInfoMapper historyInfoMapper;

    @Autowired
    private UserPow userPow;

    @Autowired
    private SortMapper sortMapper;

    @Autowired
    private LabelMapper labelMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private FamilyMapper familyMapper;

    private Searcher searcher;

    @PostConstruct
    public void init() {
        try {
            searcher = Searcher.newWithBuffer(IOUtils.toByteArray(new ClassPathResource("ip2region.xdb").getInputStream()));
        } catch (Exception e) {
        }
    }

    public void saveHistory(String ip) {
        Integer userId = PoetryUtil.getUserId();
        String ipUser = ip + (userId != null ? "_" + userId.toString() : "");

        CopyOnWriteArraySet<String> ipHistory = (CopyOnWriteArraySet<String>) PoetryCache.get(CommonConst.IP_HISTORY);
        if (!ipHistory.contains(ipUser)) {
            synchronized (ipUser.intern()) {
                if (!ipHistory.contains(ipUser)) {
                    ipHistory.add(ipUser);
                    HistoryInfoPO historyInfoPO = new HistoryInfoPO();
                    historyInfoPO.setIp(ip);
                    historyInfoPO.setUserId(userId);
                    if (searcher != null) {
                        try {
                            String search = searcher.search(ip);
                            String[] region = search.split("\\|");
                            if (!"0".equals(region[0])) {
                                historyInfoPO.setNation(region[0]);
                            }
                            if (!"0".equals(region[2])) {
                                historyInfoPO.setProvince(region[2]);
                            }
                            if (!"0".equals(region[3])) {
                                historyInfoPO.setCity(region[3]);
                            }
                        } catch (Exception e) {
                        }
                    }
                    historyInfoMapper.insert(historyInfoPO);
                }
            }
        }
    }

    public UserPO getUser(Integer userId) {
        UserPO userPO = (UserPO) PoetryCache.get(CommonConst.USER_CACHE + userId.toString());
        if (userPO != null) {
            return userPO;
        }
        UserPO u = userPow.getById(userId);
        if (u != null) {
            PoetryCache.put(CommonConst.USER_CACHE + userId.toString(), u, CommonConst.EXPIRE);
            return u;
        }
        return null;
    }

    public List<UserPO> getAdmire() {
        List<UserPO> admire = (List<UserPO>) PoetryCache.get(CommonConst.ADMIRE);
        if (admire != null) {
            return admire;
        }

        synchronized (CommonConst.ADMIRE.intern()) {
            admire = (List<UserPO>) PoetryCache.get(CommonConst.ADMIRE);
            if (admire != null) {
                return admire;
            } else {
                List<UserPO> userPOS = userPow.lambdaQuery().select(UserPO::getId, UserPO::getUsername, UserPO::getAdmire, UserPO::getAvatar).isNotNull(UserPO::getAdmire).list();

                PoetryCache.put(CommonConst.ADMIRE, userPOS, CommonConst.EXPIRE);

                return userPOS;
            }
        }
    }

    public List<FamilyVO> getFamilyList() {
        List<FamilyVO> familyVOList = (List<FamilyVO>) PoetryCache.get(CommonConst.FAMILY_LIST);
        if (familyVOList != null) {
            return familyVOList;
        }

        synchronized (CommonConst.FAMILY_LIST.intern()) {
            familyVOList = (List<FamilyVO>) PoetryCache.get(CommonConst.FAMILY_LIST);
            if (familyVOList != null) {
                return familyVOList;
            } else {
                LambdaQueryChainWrapper<FamilyPO> queryChainWrapper = new LambdaQueryChainWrapper<>(familyMapper);
                List<FamilyPO> familyPOList = queryChainWrapper.eq(FamilyPO::getStatus, Boolean.TRUE).list();
                if (!CollectionUtils.isEmpty(familyPOList)) {
                    familyVOList = familyPOList.stream().map(family -> {
                        FamilyVO familyVO = new FamilyVO();
                        BeanUtils.copyProperties(family, familyVO);
                        return familyVO;
                    }).collect(Collectors.toList());
                } else {
                    familyVOList = new ArrayList<>();
                }

                PoetryCache.put(CommonConst.FAMILY_LIST, familyVOList);
                return familyVOList;
            }
        }
    }

    public Integer getCommentCount(Integer source, String type) {
        Object count = PoetryCache.get(CommonConst.COMMENT_COUNT_CACHE + source.toString() + "_" + type);
        if (count != null) {
            return Integer.parseInt(String.valueOf(count));
        }
        LambdaQueryChainWrapper<CommentPO> wrapper = new LambdaQueryChainWrapper<>(commentMapper);
        Long c = wrapper.eq(CommentPO::getSource, source).eq(CommentPO::getType, type).count();
        PoetryCache.put(CommonConst.COMMENT_COUNT_CACHE + source.toString() + "_" + type, c, CommonConst.EXPIRE);
        return c.intValue();
    }

    public List<Integer> getUserArticleIds(Integer userId) {
        List<Integer> ids = (List<Integer>) PoetryCache.get(CommonConst.USER_ARTICLE_LIST + userId.toString());
        if (ids != null) {
            return ids;
        }

        synchronized ((CommonConst.USER_ARTICLE_LIST + userId.toString()).intern()) {
            ids = (List<Integer>) PoetryCache.get(CommonConst.USER_ARTICLE_LIST + userId.toString());
            if (ids != null) {
                return ids;
            } else {
                LambdaQueryChainWrapper<ArticlePO> wrapper = new LambdaQueryChainWrapper<>(articleMapper);
                List<ArticlePO> articlePOS = wrapper.eq(ArticlePO::getUserId, userId).select(ArticlePO::getId).list();
                List<Integer> collect = articlePOS.stream().map(ArticlePO::getId).collect(Collectors.toList());
                PoetryCache.put(CommonConst.USER_ARTICLE_LIST + userId.toString(), collect, CommonConst.EXPIRE);
                return collect;
            }
        }
    }

    public List<List<Integer>> getArticleIds(String searchText) {
        List<ArticlePO> articlePOS = (List<ArticlePO>) PoetryCache.get(CommonConst.ARTICLE_LIST);
        if (articlePOS == null) {
            synchronized (CommonConst.ARTICLE_LIST.intern()) {
                articlePOS = (List<ArticlePO>) PoetryCache.get(CommonConst.ARTICLE_LIST);
                if (articlePOS == null) {
                    LambdaQueryChainWrapper<ArticlePO> wrapper = new LambdaQueryChainWrapper<>(articleMapper);
                    articlePOS = wrapper.select(ArticlePO::getId, ArticlePO::getArticleTitle, ArticlePO::getArticleContent)
                            .orderByDesc(ArticlePO::getCreateTime)
                            .list();
                    PoetryCache.put(CommonConst.ARTICLE_LIST, articlePOS);
                }
            }
        }

        List<List<Integer>> ids = new ArrayList<>();
        List<Integer> titleIds = new ArrayList<>();
        List<Integer> contentIds = new ArrayList<>();

        for (ArticlePO articlePO : articlePOS) {
            if (StringUtil.matchString(articlePO.getArticleTitle(), searchText)) {
                titleIds.add(articlePO.getId());
            } else if (StringUtil.matchString(articlePO.getArticleContent(), searchText)) {
                contentIds.add(articlePO.getId());
            }
        }

        ids.add(titleIds);
        ids.add(contentIds);
        return ids;
    }

    public List<SortPO> getSortInfo() {
        List<SortPO> sortPOInfo = (List<SortPO>) PoetryCache.get(CommonConst.SORT_INFO);
        if (sortPOInfo != null) {
            return sortPOInfo;
        }

        synchronized (CommonConst.SORT_INFO.intern()) {
            sortPOInfo = (List<SortPO>) PoetryCache.get(CommonConst.SORT_INFO);
            if (sortPOInfo == null) {
                List<SortPO> sortPOS = new LambdaQueryChainWrapper<>(sortMapper).list();
                if (!CollectionUtils.isEmpty(sortPOS)) {
                    sortPOS.forEach(sort -> {
                        LambdaQueryChainWrapper<ArticlePO> sortWrapper = new LambdaQueryChainWrapper<>(articleMapper);
                        Long countOfSort = sortWrapper.eq(ArticlePO::getSortId, sort.getId()).count();
                        sort.setCountOfSort(countOfSort.intValue());

                        LambdaQueryChainWrapper<LabelPO> wrapper = new LambdaQueryChainWrapper<>(labelMapper);
                        List<LabelPO> labelPOS = wrapper.eq(LabelPO::getSortId, sort.getId()).list();
                        if (!CollectionUtils.isEmpty(labelPOS)) {
                            labelPOS.forEach(label -> {
                                LambdaQueryChainWrapper<ArticlePO> labelWrapper = new LambdaQueryChainWrapper<>(articleMapper);
                                Long countOfLabel = labelWrapper.eq(ArticlePO::getLabelId, label.getId()).count();
                                label.setCountOfLabel(countOfLabel.intValue());
                            });
                            sort.setLabels(labelPOS);
                        }
                    });
                }
                PoetryCache.put(CommonConst.SORT_INFO, sortPOS);
                return sortPOS;
            } else {
                return sortPOInfo;
            }
        }
    }
}

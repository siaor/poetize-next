package com.siaor.poetize.next.res.utils;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.siaor.poetize.next.app.vo.FamilyVO;
import com.siaor.poetize.next.pow.UserPow;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.res.repo.cache.SysCache;
import com.siaor.poetize.next.res.repo.mapper.*;
import com.siaor.poetize.next.res.repo.po.*;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
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
            searcher = Searcher.newWithBuffer(IOUtils.toByteArray(new FileInputStream(System.getProperty("user.dir") + File.separator + "data" + File.separator + "ip2region.xdb")));
        } catch (Exception e) {
        }
    }

    public void saveHistory(String ip) {
        Integer userId = PoetryUtil.getUserId();
        String ipUser = ip + (userId != null ? "_" + userId.toString() : "");

        CopyOnWriteArraySet<String> ipHistory = (CopyOnWriteArraySet<String>) SysCache.get(CommonConst.IP_HISTORY);
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
        UserPO userPO = (UserPO) SysCache.get(CommonConst.USER_CACHE + userId.toString());
        if (userPO != null) {
            return userPO;
        }
        UserPO u = userPow.getById(userId);
        if (u != null) {
            SysCache.put(CommonConst.USER_CACHE + userId.toString(), u, CommonConst.EXPIRE);
            return u;
        }
        return null;
    }

    public List<UserPO> getAdmire() {
        List<UserPO> admire = (List<UserPO>) SysCache.get(CommonConst.ADMIRE);
        if (admire != null) {
            return admire;
        }

        synchronized (CommonConst.ADMIRE.intern()) {
            admire = (List<UserPO>) SysCache.get(CommonConst.ADMIRE);
            if (admire != null) {
                return admire;
            } else {
                List<UserPO> userPOS = userPow.lambdaQuery().select(UserPO::getId, UserPO::getUsername, UserPO::getAdmire, UserPO::getAvatar).isNotNull(UserPO::getAdmire).list();

                SysCache.put(CommonConst.ADMIRE, userPOS, CommonConst.EXPIRE);

                return userPOS;
            }
        }
    }

    public List<FamilyVO> getFamilyList() {
        List<FamilyVO> familyVOList = (List<FamilyVO>) SysCache.get(CommonConst.FAMILY_LIST);
        if (familyVOList != null) {
            return familyVOList;
        }

        synchronized (CommonConst.FAMILY_LIST.intern()) {
            familyVOList = (List<FamilyVO>) SysCache.get(CommonConst.FAMILY_LIST);
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

                SysCache.put(CommonConst.FAMILY_LIST, familyVOList);
                return familyVOList;
            }
        }
    }

    public Integer getCommentCount(Integer source, String type) {
        Object count = SysCache.get(CommonConst.COMMENT_COUNT_CACHE + source.toString() + "_" + type);
        if (count != null) {
            return Integer.parseInt(String.valueOf(count));
        }
        LambdaQueryChainWrapper<CommentPO> wrapper = new LambdaQueryChainWrapper<>(commentMapper);
        Long c = wrapper.eq(CommentPO::getSource, source).eq(CommentPO::getType, type).count();
        SysCache.put(CommonConst.COMMENT_COUNT_CACHE + source.toString() + "_" + type, c, CommonConst.EXPIRE);
        return c.intValue();
    }

    public List<Integer> getUserArticleIds(Integer userId) {
        List<Integer> ids = (List<Integer>) SysCache.get(CommonConst.USER_ARTICLE_LIST + userId.toString());
        if (ids != null) {
            return ids;
        }

        synchronized ((CommonConst.USER_ARTICLE_LIST + userId.toString()).intern()) {
            ids = (List<Integer>) SysCache.get(CommonConst.USER_ARTICLE_LIST + userId.toString());
            if (ids != null) {
                return ids;
            } else {
                LambdaQueryChainWrapper<ArticlePO> wrapper = new LambdaQueryChainWrapper<>(articleMapper);
                List<ArticlePO> articlePOS = wrapper.eq(ArticlePO::getUserId, userId).select(ArticlePO::getId).list();
                List<Integer> collect = articlePOS.stream().map(ArticlePO::getId).collect(Collectors.toList());
                SysCache.put(CommonConst.USER_ARTICLE_LIST + userId.toString(), collect, CommonConst.EXPIRE);
                return collect;
            }
        }
    }

    public List<List<Integer>> getArticleIds(String searchText) {
        List<ArticlePO> articlePOS = (List<ArticlePO>) SysCache.get(CommonConst.ARTICLE_LIST);
        if (articlePOS == null) {
            synchronized (CommonConst.ARTICLE_LIST.intern()) {
                articlePOS = (List<ArticlePO>) SysCache.get(CommonConst.ARTICLE_LIST);
                if (articlePOS == null) {
                    LambdaQueryChainWrapper<ArticlePO> wrapper = new LambdaQueryChainWrapper<>(articleMapper);
                    articlePOS = wrapper.select(ArticlePO::getId, ArticlePO::getArticleTitle, ArticlePO::getArticleContent)
                            .orderByDesc(ArticlePO::getCreateTime)
                            .list();
                    SysCache.put(CommonConst.ARTICLE_LIST, articlePOS);
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
        List<SortPO> sortPOInfo = (List<SortPO>) SysCache.get(CommonConst.SORT_INFO);
        if (sortPOInfo != null) {
            return sortPOInfo;
        }

        synchronized (CommonConst.SORT_INFO.intern()) {
            sortPOInfo = (List<SortPO>) SysCache.get(CommonConst.SORT_INFO);
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
                SysCache.put(CommonConst.SORT_INFO, sortPOS);
                return sortPOS;
            } else {
                return sortPOInfo;
            }
        }
    }
}

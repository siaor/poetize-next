package com.siaor.poetize.next.pow;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.repo.po.ArticlePO;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.baomidou.mybatisplus.extension.service.IService;
import com.siaor.poetize.next.app.vo.ArticleVO;
import com.siaor.poetize.next.app.vo.BaseRequestVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文章表 服务类
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
public interface ArticlePow extends IService<ArticlePO> {

    PoetryResult saveArticle(ArticleVO articleVO);

    PoetryResult deleteArticle(Integer id);

    PoetryResult updateArticle(ArticleVO articleVO);

    PoetryResult reload();

    PoetryResult<Page> listArticle(BaseRequestVO baseRequestVO);

    PoetryResult<ArticleVO> getArticleById(Integer id, String password);

    PoetryResult<Page> listAdminArticle(BaseRequestVO baseRequestVO, Boolean isBoss);

    PoetryResult<ArticleVO> getArticleByIdForUser(Integer id);

    PoetryResult<Map<Integer, List<ArticleVO>>> listSortArticle();
}

package com.siaor.poetize.next.pow;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.res.repo.po.ArticlePO;
import com.siaor.poetize.next.res.norm.ActResult;
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

    ActResult saveArticle(ArticleVO articleVO);

    ActResult deleteArticle(Integer id);

    ActResult updateArticle(ArticleVO articleVO);

    ActResult reload();

    ActResult<Page> listArticle(BaseRequestVO baseRequestVO);

    ActResult<ArticleVO> getArticleById(Integer id, String password);

    ActResult<Page> listAdminArticle(BaseRequestVO baseRequestVO, Boolean isBoss);

    ActResult<ArticleVO> getArticleByIdForUser(Integer id);

    ActResult<Map<Integer, List<ArticleVO>>> listSortArticle();
}

package com.siaor.poetize.next.app.api.blog;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.res.oper.aop.LoginCheck;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.CommonConst;
import com.siaor.poetize.next.pow.ArticlePow;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import com.siaor.poetize.next.res.repo.cache.SysCache;
import com.siaor.poetize.next.res.task.ArticleScanTask;
import com.siaor.poetize.next.app.vo.ArticleVO;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


/**
 * <p>
 * 文章表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/article")
public class ArticleApi {

    @Autowired
    private ArticlePow articlePow;

    @Autowired
    private ArticleScanTask articleScanTask;


    /**
     * 保存文章
     */
    @LoginCheck(1)
    @PostMapping("/saveArticle")
    public ActResult saveArticle(@Validated @RequestBody ArticleVO articleVO) {
        SysCache.remove(CommonConst.USER_ARTICLE_LIST + PoetryUtil.getUserId().toString());
        SysCache.remove(CommonConst.ARTICLE_LIST);
        SysCache.remove(CommonConst.SORT_ARTICLE_LIST);
        return articlePow.saveArticle(articleVO);
    }


    /**
     * 删除文章
     */
    @GetMapping("/deleteArticle")
    @LoginCheck(1)
    public ActResult deleteArticle(@RequestParam("id") Integer id) {
        SysCache.remove(CommonConst.USER_ARTICLE_LIST + PoetryUtil.getUserId().toString());
        SysCache.remove(CommonConst.ARTICLE_LIST);
        SysCache.remove(CommonConst.SORT_ARTICLE_LIST);
        return articlePow.deleteArticle(id);
    }


    /**
     * 更新文章
     */
    @PostMapping("/updateArticle")
    @LoginCheck(1)
    public ActResult updateArticle(@Validated @RequestBody ArticleVO articleVO) {
        SysCache.remove(CommonConst.ARTICLE_LIST);
        SysCache.remove(CommonConst.SORT_ARTICLE_LIST);
        return articlePow.updateArticle(articleVO);
    }

    /**
     * 重新加载文章
     *
     * @author Siaor
     * @since 2025-02-23 05:54:41
     */
    @PostMapping("/reload")
    @LoginCheck(1)
    public ActResult reloadArticle() {
        SysCache.remove(CommonConst.USER_ARTICLE_LIST + PoetryUtil.getUserId().toString());
        SysCache.remove(CommonConst.ARTICLE_LIST);
        SysCache.remove(CommonConst.SORT_ARTICLE_LIST);
        //判断重载任务是否处理完毕
        int taskSize = articleScanTask.getSize();
        if (taskSize > 0) {
            return ActResult.success("正在重载数据中，剩余：" + taskSize);
        }
        return articlePow.reload();
    }

    /**
     * 获取重载任务数量
     *
     * @author Siaor
     * @since 2025-03-10 08:48:09
     */
    @GetMapping("/reload/task")
    @LoginCheck(1)
    public ActResult getReloadTask() {
        return ActResult.success(articleScanTask.getSize());
    }

    /**
     * 查询文章List
     */
    @PostMapping("/listArticle")
    public ActResult<Page> listArticle(@RequestBody BaseRequestVO baseRequestVO) {
        return articlePow.listArticle(baseRequestVO);
    }

    /**
     * 查询分类文章List
     */
    @GetMapping("/listSortArticle")
    public ActResult<Map<Integer, List<ArticleVO>>> listSortArticle() {
        return articlePow.listSortArticle();
    }

    /**
     * 查询文章
     */
    @GetMapping("/getArticleById")
    public ActResult<ArticleVO> getArticleById(@RequestParam("id") Integer id, @RequestParam(value = "password", required = false) String password) {
        return articlePow.getArticleById(id, password);
    }
}


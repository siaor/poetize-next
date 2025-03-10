package com.siaor.poetize.next.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.aop.LoginCheck;
import com.siaor.poetize.next.config.PoetryResult;
import com.siaor.poetize.next.constants.CommonConst;
import com.siaor.poetize.next.service.ArticleService;
import com.siaor.poetize.next.utils.PoetryUtil;
import com.siaor.poetize.next.utils.cache.PoetryCache;
import com.siaor.poetize.next.utils.storage.ArticleScanTask;
import com.siaor.poetize.next.vo.ArticleVO;
import com.siaor.poetize.next.vo.BaseRequestVO;
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
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleScanTask articleScanTask;


    /**
     * 保存文章
     */
    @LoginCheck(1)
    @PostMapping("/saveArticle")
    public PoetryResult saveArticle(@Validated @RequestBody ArticleVO articleVO) {
        PoetryCache.remove(CommonConst.USER_ARTICLE_LIST + PoetryUtil.getUserId().toString());
        PoetryCache.remove(CommonConst.ARTICLE_LIST);
        PoetryCache.remove(CommonConst.SORT_ARTICLE_LIST);
        return articleService.saveArticle(articleVO);
    }


    /**
     * 删除文章
     */
    @GetMapping("/deleteArticle")
    @LoginCheck(1)
    public PoetryResult deleteArticle(@RequestParam("id") Integer id) {
        PoetryCache.remove(CommonConst.USER_ARTICLE_LIST + PoetryUtil.getUserId().toString());
        PoetryCache.remove(CommonConst.ARTICLE_LIST);
        PoetryCache.remove(CommonConst.SORT_ARTICLE_LIST);
        return articleService.deleteArticle(id);
    }


    /**
     * 更新文章
     */
    @PostMapping("/updateArticle")
    @LoginCheck(1)
    public PoetryResult updateArticle(@Validated @RequestBody ArticleVO articleVO) {
        PoetryCache.remove(CommonConst.ARTICLE_LIST);
        PoetryCache.remove(CommonConst.SORT_ARTICLE_LIST);
        return articleService.updateArticle(articleVO);
    }

    /**
     * 重新加载文章
     *
     * @author Siaor
     * @since 2025-02-23 05:54:41
     */
    @PostMapping("/reload")
    @LoginCheck(1)
    public PoetryResult reloadArticle() {
        PoetryCache.remove(CommonConst.USER_ARTICLE_LIST + PoetryUtil.getUserId().toString());
        PoetryCache.remove(CommonConst.ARTICLE_LIST);
        PoetryCache.remove(CommonConst.SORT_ARTICLE_LIST);
        //判断重载任务是否处理完毕
        int taskSize = articleScanTask.getSize();
        if (taskSize > 0) {
            return PoetryResult.success("正在重载数据中，剩余：" + taskSize);
        }
        return articleService.reload();
    }

    /**
     * 获取重载任务数量
     *
     * @author Siaor
     * @since 2025-03-10 08:48:09
     */
    @GetMapping("/reload/task")
    @LoginCheck(1)
    public PoetryResult getReloadTask() {
        return PoetryResult.success(articleScanTask.getSize());
    }

    /**
     * 查询文章List
     */
    @PostMapping("/listArticle")
    public PoetryResult<Page> listArticle(@RequestBody BaseRequestVO baseRequestVO) {
        return articleService.listArticle(baseRequestVO);
    }

    /**
     * 查询分类文章List
     */
    @GetMapping("/listSortArticle")
    public PoetryResult<Map<Integer, List<ArticleVO>>> listSortArticle() {
        return articleService.listSortArticle();
    }

    /**
     * 查询文章
     */
    @GetMapping("/getArticleById")
    public PoetryResult<ArticleVO> getArticleById(@RequestParam("id") Integer id, @RequestParam(value = "password", required = false) String password) {
        return articleService.getArticleById(id, password);
    }
}


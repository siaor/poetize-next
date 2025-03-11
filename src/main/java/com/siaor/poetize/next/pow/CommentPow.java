package com.siaor.poetize.next.pow;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.repo.po.CommentPO;
import com.siaor.poetize.next.res.config.PoetryResult;
import com.baomidou.mybatisplus.extension.service.IService;
import com.siaor.poetize.next.app.vo.BaseRequestVO;
import com.siaor.poetize.next.app.vo.CommentVO;


/**
 * <p>
 * 文章评论表 服务类
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
public interface CommentPow extends IService<CommentPO> {

    PoetryResult saveComment(CommentVO commentVO);

    PoetryResult deleteComment(Integer id);

    PoetryResult<BaseRequestVO> listComment(BaseRequestVO baseRequestVO);

    PoetryResult<Page> listAdminComment(BaseRequestVO baseRequestVO, Boolean isBoss);
}

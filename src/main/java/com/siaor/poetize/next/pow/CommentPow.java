package com.siaor.poetize.next.pow;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siaor.poetize.next.res.repo.po.CommentPO;
import com.siaor.poetize.next.res.norm.ActResult;
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

    ActResult saveComment(CommentVO commentVO);

    ActResult deleteComment(Integer id);

    ActResult<BaseRequestVO> listComment(BaseRequestVO baseRequestVO);

    ActResult<Page> listAdminComment(BaseRequestVO baseRequestVO, Boolean isBoss);
}

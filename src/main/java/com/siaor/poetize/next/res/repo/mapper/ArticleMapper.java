package com.siaor.poetize.next.res.repo.mapper;

import com.siaor.poetize.next.res.repo.po.ArticlePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 文章表 Mapper 接口
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
public interface ArticleMapper extends BaseMapper<ArticlePO> {

    @Update("update article set view_count=view_count+1 where id=#{id}")
    int updateViewCount(@Param("id") Integer id);
}

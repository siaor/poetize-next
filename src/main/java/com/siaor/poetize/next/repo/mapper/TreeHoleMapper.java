package com.siaor.poetize.next.repo.mapper;

import com.siaor.poetize.next.repo.po.TreeHolePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 树洞 Mapper 接口
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
public interface TreeHoleMapper extends BaseMapper<TreeHolePO> {

    List<TreeHolePO> queryAllByLimit(@Param("offset") int offset, @Param("limit") int limit);

}

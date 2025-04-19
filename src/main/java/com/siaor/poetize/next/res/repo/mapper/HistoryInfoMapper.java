package com.siaor.poetize.next.res.repo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siaor.poetize.next.res.repo.po.HistoryInfoPO;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 历史信息 Mapper 接口
 * </p>
 *
 * @author sara
 * @since 2023-07-24
 */
public interface HistoryInfoMapper extends BaseMapper<HistoryInfoPO> {

    /**
     * 访问IP最多的10个省
     */
    @Select("select nation, province, count(distinct ip) as num" +
            " from history_info" +
            " where nation is not null and province is not null" +
            " group by nation, province" +
            " order by num desc" +
            " limit 10")
    List<Map<String, Object>> getHistoryByProvince();

    /**
     * 访问次数最多的10个IP
     */
    @Select("select ip, min(province) as province, min(city) as city, count(*) as num" +
            " from history_info" +
            " group by ip" +
            " order by num desc" +
            " limit 10")
    List<Map<String, Object>> getHistoryByIp();

    /**
     * 访问24小时内的数据
     */
    @Select("select ip, user_id, nation, province, city" +
            " from history_info" +
            " where create_time >= (now() - interval '24' hour)")
    List<Map<String, Object>> getHistoryBy24Hour();

    /**
     * 总访问量
     */
    @Select("select count(*) from history_info")
    Long getHistoryCount();
}

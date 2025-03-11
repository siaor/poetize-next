package com.siaor.poetize.next.pro;

import com.siaor.poetize.next.repo.po.HistoryInfoPO;
import com.siaor.poetize.next.repo.mapper.HistoryInfoMapper;
import com.siaor.poetize.next.pow.HistoryInfoPow;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 历史信息 服务实现类
 * </p>
 *
 * @author sara
 * @since 2023-07-24
 */
@Service
public class HistoryInfoPowPro extends ServiceImpl<HistoryInfoMapper, HistoryInfoPO> implements HistoryInfoPow {

}

package com.siaor.poetize.next.pro;

import com.siaor.poetize.next.repo.po.SysConfigPO;
import com.siaor.poetize.next.repo.mapper.SysConfigMapper;
import com.siaor.poetize.next.pow.SysConfigPow;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 参数配置表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2024-03-23
 */
@Service
public class SysConfigPowPro extends ServiceImpl<SysConfigMapper, SysConfigPO> implements SysConfigPow {

}

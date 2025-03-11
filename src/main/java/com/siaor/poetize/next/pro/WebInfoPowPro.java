package com.siaor.poetize.next.pro;

import com.siaor.poetize.next.repo.po.WebInfoPO;
import com.siaor.poetize.next.repo.mapper.WebInfoMapper;
import com.siaor.poetize.next.pow.WebInfoPow;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 网站信息表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@Service
public class WebInfoPowPro extends ServiceImpl<WebInfoMapper, WebInfoPO> implements WebInfoPow {

}

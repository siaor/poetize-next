package com.siaor.poetize.next.pro;

import com.siaor.poetize.next.res.repo.po.ResourcePO;
import com.siaor.poetize.next.res.repo.mapper.ResourceMapper;
import com.siaor.poetize.next.pow.ResourcePow;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 资源信息 服务实现类
 * </p>
 *
 * @author sara
 * @since 2022-03-06
 */
@Service
public class ResourcePowPro extends ServiceImpl<ResourceMapper, ResourcePO> implements ResourcePow {

}

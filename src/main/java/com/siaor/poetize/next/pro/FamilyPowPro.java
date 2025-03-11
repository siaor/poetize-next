package com.siaor.poetize.next.pro;

import com.siaor.poetize.next.repo.po.FamilyPO;
import com.siaor.poetize.next.repo.mapper.FamilyMapper;
import com.siaor.poetize.next.pow.FamilyPow;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 家庭信息 服务实现类
 * </p>
 *
 * @author sara
 * @since 2023-01-03
 */
@Service
public class FamilyPowPro extends ServiceImpl<FamilyMapper, FamilyPO> implements FamilyPow {

}

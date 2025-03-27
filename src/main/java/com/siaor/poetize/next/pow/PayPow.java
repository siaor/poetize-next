package com.siaor.poetize.next.pow;

import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.repo.po.PayOrderPO;

import java.math.BigDecimal;

/**
 * 支付能力
 *
 * @author Siaor
 * @since 2025-03-26 01:03:10
 */
public interface PayPow {

    /**
     * 创建支付订单
     * @param actType 操作类型
     * @param actId 操作ID
     * @param money 金额
     * @return 支付订单信息
     */
    ActResult<PayOrderPO> createPayOrder(Integer actType,Integer actId, BigDecimal money);

    /**
     * 获取支付订单
     * @param payOrderId 支付订单ID
     * @param actType 操作类型
     * @param actId 操作ID
     * @param money 金额
     * @return 支付订单信息
     */
    ActResult<PayOrderPO> getPayOrder(Long payOrderId,Integer actType,Integer actId, BigDecimal money);

    /**
     * 取消支付订单
     * @param payOrderId 支付订单ID
     * @param actType 操作类型
     * @param actId 操作ID
     * @param money 金额
     * @return 结果
     */
    ActResult<String> cancelPayOrder(Long payOrderId,Integer actType,Integer actId, BigDecimal money);
}

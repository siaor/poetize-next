package com.siaor.poetize.next.res.norm;

/**
 * 支付订单状态
 *
 * @author Siaor
 * @since 2025-03-26 03:37:21
 */
public class PayOrderStatus {
    /**
     * 0.未支付
     * 1.已支付
     * 2.取消支付
     * 3.支付超时
     * 4.支付失败
     */
    public static final int UNPAID = 0;
    public static final int PAID = 1;
    public static final int CANCEL = 2;
    public static final int TIMEOUT = 3;
    public static final int FAILURE = 4;
}

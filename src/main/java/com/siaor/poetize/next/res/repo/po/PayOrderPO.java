package com.siaor.poetize.next.res.repo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付订单
 *
 * @author Siaor
 * @since 2025-03-26 02:41:15
 */
@Data
@TableName("pay_order")
public class PayOrderPO implements Serializable {
    @Serial
    private static final long serialVersionUID = -1650403143730447378L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Date createTime;
    private Date updateTime;
    private String num;
    private Integer status;
    private Integer userId;
    private Integer actType;
    private Integer actId;
    private BigDecimal money;
    private String payUrl;
    private String payNum;
    private String payUserId;
    private Date payTime;
    private BigDecimal payMoney;
    private String note;
}

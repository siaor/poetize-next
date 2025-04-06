package com.siaor.poetize.next.pro;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alipay.api.domain.AccountLogItemResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siaor.poetize.next.pow.PayPow;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.PayOrderStatus;
import com.siaor.poetize.next.res.oper.AliPayHandler;
import com.siaor.poetize.next.res.repo.mapper.PayOrderMapper;
import com.siaor.poetize.next.res.repo.po.PayOrderPO;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 支付能力实现
 *
 * @author Siaor
 * @since 2025-03-26 01:03:19
 */
@Service
@Slf4j
public class PayPowPro implements PayPow {
    @Value("${alipay.userId}")
    private String payUserId;

    @Resource
    private PayOrderMapper payOrderMapper;

    @Resource
    private AliPayHandler aliPayHandler;

    @Override
    public ActResult<PayOrderPO> createPayOrder(Integer actType, Integer actId, BigDecimal money) {
        if (!StringUtils.hasText(payUserId)) {
            return ActResult.fail("未设置收款账号");
        }

        Integer userId = PoetryUtil.getUserId();
        if (userId == null) {
            userId = 0;
        }

        String payNum = genPayNum();

        String payUrl = buildPayUrl(payNum, money);

        // 创建支付订单
        PayOrderPO payOrder = new PayOrderPO();
        payOrder.setCreateTime(new Date());
        payOrder.setUpdateTime(new Date());
        payOrder.setNum(payNum);
        payOrder.setStatus(PayOrderStatus.UNPAID);
        payOrder.setUserId(userId);
        payOrder.setActType(actType);
        payOrder.setActId(actId);
        payOrder.setMoney(money);

        payOrder.setPayUrl(payUrl);
        payOrder.setPayNum("");
        payOrder.setPayUserId("");
        payOrder.setPayTime(new Date(0));
        payOrder.setPayMoney(BigDecimal.ZERO);
        payOrder.setNote("");

        payOrderMapper.insert(payOrder);

        // 创建支付检测任务
        payChecker(payOrder);

        return ActResult.success(payOrder);
    }

    @Override
    public ActResult<PayOrderPO> getPayOrder(Long payOrderId, Integer actType, Integer actId, BigDecimal money) {
        Integer userId = PoetryUtil.getUserId();
        if (userId == null) {
            userId = 0;
        }
        Date endTime = new Date();
        Date startTime = DateUtil.offsetMinute(endTime, -10);
        QueryWrapper<PayOrderPO> payQW = new QueryWrapper<>();
        payQW.eq("id", payOrderId)
                .eq("act_type", actType)
                .eq("act_id", actId)
                .eq("user_id", userId)
                .between("create_time", startTime, endTime);

        if (money != null) {
            payQW.eq("money", money);
        }

        PayOrderPO payOrder = payOrderMapper.selectOne(payQW);
        return ActResult.success(payOrder);
    }

    @Override
    public ActResult<String> cancelPayOrder(Long payOrderId, Integer actType, Integer actId, BigDecimal money) {
        Integer userId = PoetryUtil.getUserId();
        if (userId == null) {
            userId = 0;
        }
        QueryWrapper<PayOrderPO> payQW = new QueryWrapper<>();
        payQW.eq("id", payOrderId)
                .eq("act_type", actType)
                .eq("act_id", actId)
                .eq("user_id", userId)
                .eq("status", PayOrderStatus.UNPAID);

        if (money != null) {
            payQW.eq("money", money);
        }

        PayOrderPO payOrder = payOrderMapper.selectOne(payQW);
        if (payOrder == null) {
            return ActResult.fail("找不到相关订单");
        }
        PayOrderPO updateInfo = new PayOrderPO();
        updateInfo.setId(payOrder.getId());
        updateInfo.setUpdateTime(new Date());
        updateInfo.setStatus(PayOrderStatus.CANCEL);
        payOrderMapper.updateById(updateInfo);
        return ActResult.success();
    }

    /**
     * 支付检测器
     */
    private void payChecker(PayOrderPO payOrder) {
        if (!aliPayHandler.isReady()) {
            return;
        }

        AtomicInteger count = new AtomicInteger(1);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            log.info("开始检测订单支付状态：{}", payOrder.getNum());
            // 检测订单是否手动取消
            QueryWrapper<PayOrderPO> payQW = new QueryWrapper<>();
            payQW.eq("id", payOrder.getId()).eq("status", PayOrderStatus.CANCEL);
            if (payOrderMapper.exists(payQW)) {
                log.info("支付取消，订单：{}", payOrder.getNum());
                scheduler.shutdown();
                return;
            }

            // 检测支付信息
            AccountLogItemResult payInfo = aliPayHandler.getPayInfo(payOrder);
            if (payInfo != null) {
                // 应付金额
                BigDecimal money = payOrder.getMoney();

                // 实际支付金额
                BigDecimal payMoney = new BigDecimal(payInfo.getTransAmount());

                PayOrderPO updateInfo = new PayOrderPO();
                updateInfo.setId(payOrder.getId());
                updateInfo.setUpdateTime(new Date());
                updateInfo.setPayNum(payInfo.getAlipayOrderNo());
                updateInfo.setPayUserId(payInfo.getOtherAccount());
                updateInfo.setPayTime(DateUtil.parse(payInfo.getTransDt(), DatePattern.NORM_DATETIME_PATTERN));
                updateInfo.setPayMoney(payMoney);
                updateInfo.setNote(payInfo.getAccountLogId());

                // 设置了应付金额时，实际支付金额需要大于等于应付金额
                if (money.compareTo(BigDecimal.ZERO) > 0 && payMoney.compareTo(money) < 0) {
                    // 支付金额不足，更新已付金额，并设置状态为失败
                    log.info("您支付的金额不足，请重新支付！如需退回已付金额，请联系管理员，订单：{}", payOrder.getNum());
                    updateInfo.setStatus(PayOrderStatus.FAILURE);
                    payOrderMapper.updateById(updateInfo);
                    scheduler.shutdown();
                    return;
                }

                // 支付成功
                log.info("支付成功，订单：{}", payOrder.getNum());
                updateInfo.setStatus(PayOrderStatus.PAID);
                payOrderMapper.updateById(updateInfo);
                scheduler.shutdown();
                return;
            }

            // 超过5分钟，自动取消支付订单
            if (count.get() > 60) {
                log.info("支付超时，订单：{}", payOrder.getNum());
                // 更新订单状态
                PayOrderPO updateInfo = new PayOrderPO();
                updateInfo.setId(payOrder.getId());
                updateInfo.setUpdateTime(new Date());
                updateInfo.setStatus(PayOrderStatus.TIMEOUT);
                payOrderMapper.updateById(updateInfo);

                // 结束任务
                scheduler.shutdownNow();
                return;
            }
            count.getAndIncrement();
        }, 10, 5, TimeUnit.SECONDS);
    }

    /**
     * 生成支付链接
     */
    public String buildPayUrl(String payNum, BigDecimal payMoney) {
        String payUrl = "https://ds.alipay.com/?from=pc&appId=20000116&actionType=toAccount&goBack=NO";
        if (StringUtils.hasText(payNum)) {
            payUrl = payUrl + "&memo=" + payNum;
        }
        if (payMoney != null && payMoney.compareTo(BigDecimal.ZERO) > 0) {
            payUrl = payUrl + "&amount=" + payMoney.setScale(2, RoundingMode.HALF_UP);
        }
        return payUrl + "&userId=" + payUserId;
    }

    /**
     * 生成支付订单号
     */
    private String genPayNum() {
        return "PN" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_MS_FORMAT) + String.format("%05d", new Random().nextInt(10000));
    }
}

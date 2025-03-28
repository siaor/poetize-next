package com.siaor.poetize.next.res.oper;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AccountLogItemResult;
import com.alipay.api.request.AlipayDataBillAccountlogQueryRequest;
import com.alipay.api.response.AlipayDataBillAccountlogQueryResponse;
import com.siaor.poetize.next.res.repo.po.PayOrderPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 支付宝处理器
 *
 * @author Siaor
 * @since 2025-03-26 05:26:55
 */
@Component
@Slf4j
public class AliPayHandler {

    @Value("${alipay.serverUrl}")
    private String serverUrl;

    @Value("${alipay.publicKey}")
    private String publicKey;

    @Value("${alipay.userId}")
    private String userId;

    @Value("${alipay.appId}")
    private String appId;

    @Value("${alipay.appKey}")
    private String appKey;

    public String format = "JSON";
    public String charset = "UTF-8";
    public String signType = "RSA2";

    /**
     * 通过Alipay SDK获取订单信息
     * todo: 自定义实现调用（阿里支付SDK太臃肿了，打包占用了30M）
     */
    public AccountLogItemResult getPayInfo(PayOrderPO payOrder) {
        String payOrderNum = payOrder.getNum();
        Date startTime = DateUtil.offsetMinute(payOrder.getCreateTime(), -1);
        Date endTime = DateUtil.offsetMinute(new Date(), 2);
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, appId, appKey, format, charset, publicKey, signType);
            AlipayDataBillAccountlogQueryRequest request = new AlipayDataBillAccountlogQueryRequest();
            //查询参数
            JSONObject bizQw = new JSONObject();
            bizQw.put("start_time", DateUtil.format(startTime, DatePattern.NORM_DATETIME_FORMATTER));
            bizQw.put("end_time", DateUtil.format(endTime, DatePattern.NORM_DATETIME_FORMATTER));
            bizQw.put("page_no", "1");
            bizQw.put("page_size", "2000");
            bizQw.put("bill_user_id", userId);
            request.setBizContent(bizQw.toString());

            //查询账单列表
            AlipayDataBillAccountlogQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                List<AccountLogItemResult> payList = response.getDetailList();
                if(payList == null || payList.isEmpty()){
                    return null;
                }
                return payList.stream().filter(p -> payOrderNum.equals(p.getTransMemo())).findFirst().orElse(null);
            } else {
                log.info(response.getMsg());
                return null;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }
}

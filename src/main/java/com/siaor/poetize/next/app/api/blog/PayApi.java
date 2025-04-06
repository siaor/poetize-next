package com.siaor.poetize.next.app.api.blog;

import com.siaor.poetize.next.pow.ArticlePow;
import com.siaor.poetize.next.pow.PayPow;
import com.siaor.poetize.next.res.norm.ActCode;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.PayOrderActType;
import com.siaor.poetize.next.res.norm.PayOrderStatus;
import com.siaor.poetize.next.res.repo.po.ArticlePO;
import com.siaor.poetize.next.res.repo.po.PayOrderPO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 付款接口
 *
 * @author Siaor
 * @since 2025-03-26 10:17:07
 */
@RestController
public class PayApi {

    @Resource
    private PayPow payPow;

    @Resource
    private ArticlePow articlePow;

    /**
     * 获取打赏支付信息
     */
    @GetMapping("/reward/order/create")
    public ActResult<PayOrderPO> createRewardPayOrder(@RequestParam("articleId") Integer articleId) {
        return payPow.createPayOrder(PayOrderActType.REWARD, articleId, BigDecimal.ZERO);
    }

    /**
     * 获取打赏支付订单信息
     */
    @GetMapping("/reward/order/get")
    public ActResult<PayOrderPO> getRewardPayOrder(@RequestParam("id") Long id, @RequestParam("articleId") Integer articleId) {
        return payPow.getPayOrder(id, PayOrderActType.REWARD, articleId, BigDecimal.ZERO);
    }

    /**
     * 取消打赏支付
     */
    @PostMapping("/reward/order/cancel")
    public ActResult<String> cancelRewardPayOrder(@RequestBody PayOrderPO p) {
        return payPow.cancelPayOrder(p.getId(), PayOrderActType.REWARD, p.getActId(), BigDecimal.ZERO);
    }

    /**
     * 获取文章支付信息
     */
    @GetMapping("/article/order/create")
    public ActResult<PayOrderPO> createArticlePayOrder(@RequestParam("articleId") Integer articleId) {
        ArticlePO article = articlePow.getById(articleId);
        if (article == null) {
            return ActResult.fail(ActCode.RES_LOSE);
        }

        return payPow.createPayOrder(PayOrderActType.ARTICLE, articleId, article.getMoney());
    }

    /**
     * 获取文章支付订单信息
     */
    @GetMapping("/article/order/get")
    public ActResult<PayOrderPO> getArticlePayOrder(@RequestParam("id") Long id, @RequestParam("articleId") Integer articleId) {
        ActResult<PayOrderPO> result = payPow.getPayOrder(id, PayOrderActType.ARTICLE, articleId, null);
        PayOrderPO payOrder = result.getData();
        if (payOrder != null && PayOrderStatus.PAID == payOrder.getStatus()) {
            //支付成功，将文章密码附带返回
            ArticlePO article = articlePow.getById(articleId);
            if (article != null) {
                payOrder.setNote(article.getPassword());
            }
        }
        return result;
    }

    /**
     * 取消文章支付
     */
    @PostMapping("/article/order/cancel")
    public ActResult<String> cancelArticlePayOrder(@RequestBody PayOrderPO p) {
        return payPow.cancelPayOrder(p.getId(), PayOrderActType.ARTICLE, p.getActId(), null);
    }
}

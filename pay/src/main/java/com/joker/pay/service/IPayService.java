package com.joker.pay.service;

import com.joker.pay.pojo.PayInfo;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayResponse;

import javax.lang.model.element.NestingKind;
import java.math.BigDecimal;

public interface IPayService {

    /**
     * 创建/发起支付
     * @param orderId 订单号
     * @param amount 支付金额
     */
    PayResponse create(String orderId, BigDecimal amount, BestPayTypeEnum bestPayTypeEnum);

    /**
     * 异步通知处理
     * @param notifyData
     */
    String asyncNotify(String notifyData);

    PayInfo queryByOrderId(String orderId);
}

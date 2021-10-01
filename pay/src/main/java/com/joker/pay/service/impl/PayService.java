package com.joker.pay.service.impl;

import com.google.gson.Gson;
import com.joker.pay.dao.PayInfoMapper;
import com.joker.pay.enums.PayPlatformEnum;
import com.joker.pay.pojo.PayInfo;
import com.joker.pay.service.IPayService;
import com.lly835.bestpay.enums.BestPayPlatformEnum;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.enums.OrderStatusEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.jms.Queue;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class PayService implements IPayService {

    @Autowired
    private BestPayService PayService ;

    @Autowired
    private PayInfoMapper payInfoMapper;

    private Queue queue = new ActiveMQQueue("pay_queue");

    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    @Override
    public PayResponse create(String orderId, BigDecimal amount,BestPayTypeEnum bestPayTypeEnum) {

        //写入数据库
        PayInfo payInfo = new PayInfo(Long.parseLong(orderId),
                PayPlatformEnum.getByBestPayTypeEnum(bestPayTypeEnum).getCode(),
                OrderStatusEnum.NOTPAY.name(),
                amount);
        payInfoMapper.insertSelective(payInfo);


        PayRequest request = new PayRequest();
        request.setOrderName("7195223-最好的支付sdk");
        request.setOrderId(orderId);
        request.setOrderAmount(amount.doubleValue());
        request.setPayTypeEnum(bestPayTypeEnum);



        PayResponse payResponse = PayService.pay(request);
        log.info("responce={}",payResponse);

        return payResponse;

    }

    @Override
    public String asyncNotify(String notifyData) {
        //1.签名校验
        PayResponse payResponse = PayService.asyncNotify(notifyData);
        log.info("payResponse={}",payResponse);
        //2.金额校验
        PayInfo payInfo = payInfoMapper.selectByOrderNo(Long.parseLong(payResponse.getOrderId()));
        if(payInfo == null){
            throw new RuntimeException("订单异常");
        }
        //订单支付状态不是已支付
        if(!payInfo.getPlatformStatus().equals(OrderStatusEnum.SUCCESS)){
            if (payInfo.getPayAmount().compareTo(BigDecimal.valueOf(payResponse.getOrderAmount())) != 0){
                //告警
                throw new RuntimeException("异步通知中的金额和数据库里的金额不一致,orderNo="+payResponse.getOrderId());
            }

            //3.修改订单支付状态
            payInfo.setPlatformStatus(OrderStatusEnum.SUCCESS.name());
            payInfo.setPlatformNumber(payResponse.getOutTradeNo());
            payInfo.setUpdateTime(null);
            payInfoMapper.updateByPrimaryKeySelective(payInfo);
        }

        //pay发送MQ消息，mall接受MQ消息
        jmsMessagingTemplate.convertAndSend( queue, new Gson().toJson(payInfo));


        if(payResponse.getPayPlatformEnum() == BestPayPlatformEnum.WX){

            //4.告诉微信不要再通知了
            return "<xml>\n" +
                    "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                    "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                    "</xml>";
        }
        else if(payResponse.getPayPlatformEnum() == BestPayPlatformEnum.ALIPAY){
            //4.告诉支付宝不要再通知了
            return "success";
        }
        throw new RuntimeException("异步通知中错误的支付平台");
    }

    @Override
    public PayInfo queryByOrderId(String orderId) {

        return  payInfoMapper.selectByOrderNo(Long.parseLong(orderId));

    }
}

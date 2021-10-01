package com.joker.pay.controller;

import com.joker.pay.pojo.PayInfo;
import com.joker.pay.service.impl.PayService;
import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private PayService payService;

    @Autowired
    private WxPayConfig wxPayConfig;

    @GetMapping("/create")
    public ModelAndView create(@RequestParam("orderId")String orderId,
                                @RequestParam("amount")BigDecimal amount,
                                @RequestParam("payType")BestPayTypeEnum bestPayTypeEnum){

        PayResponse response = payService.create(orderId, amount,bestPayTypeEnum);

        Map map = new HashMap<>();

        //支付方式不同，渲染就不同，WXPAY_NATIVE使用codeUrl，ALIPAY_PC使用body
        if(bestPayTypeEnum == BestPayTypeEnum.WXPAY_NATIVE){

            map.put("codeUrl",response.getCodeUrl());
            map.put("orderId",orderId);
            map.put("returnUrl",wxPayConfig.getReturnUrl());
            return new ModelAndView("createForWxNative",map);
        }else if(bestPayTypeEnum == BestPayTypeEnum.ALIPAY_PC){
            map.put("body",response.getBody());
            return  new ModelAndView("createForAlipayPc",map);
        }

        throw new RuntimeException("暂时不支持其他支付");

    }
    @PostMapping("/notify")
    @ResponseBody
    public String asynNotify(@RequestBody String notifyData){

        return payService.asyncNotify(notifyData);
    }

    @GetMapping("/queryByOrderId")
    @ResponseBody
    public PayInfo queryByOrderId(@RequestParam String orderId){
        return payService.queryByOrderId(orderId);

    }
}

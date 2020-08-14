package com.cicada.component.wxpay.interfaces;

import com.cicada.component.wxpay.WxpayAbstractPay;
import com.cicada.component.wxpay.bean.WxpayConfig;
import com.cicada.component.wxpay.enums.WxpayAction;
import com.cicada.component.wxpay.enums.WxpayParam;
import com.cicada.component.wxpay.support.WxpayRequester;
import com.cicada.core.annotation.Component;
import com.cicada.core.bean.TradingContext;
import com.cicada.core.bean.TradingResult;
import com.cicada.core.enums.Channel;
import com.cicada.core.enums.ComponetType;
import com.cicada.core.enums.ProductType;
import com.cicada.core.enums.TradingStatus;
import com.cicada.core.exception.business.IllegalTradingStatusException;
import com.cicada.core.requester.RequestExecutor;
import com.cicada.storage.PayPersistService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付交易撤销
 * 支付交易返回失败或支付系统超时，调用该接口撤销交易。如果此订单用户支付失败，
 * 微信支付系统会将此订单关闭；如果用户支付成功，微信支付系统会将此订单资金退还给用户。
 * <p/>
 * 注意：7天以内的交易单可调用撤销，其他正常支付的单如需实现相同功能请调用申请退款API。
 * 提交支付交易后调用【查询订单API】，没有明确的支付结果再调用【撤销订单API】。
 *
 * @author: WUJXIAO
 * @create: 2018-12-13 08:58
 * @version: 1.0
 **/
@Component(value = ComponetType.CANCEL, channel = Channel.WXPAY, product = ProductType.CANCEL)
public class WxpayCancel extends WxpayAbstractPay {

    @Autowired
    private PayPersistService persistService;

    @Override
    protected String getAction() {
        return WxpayAction.CANCEL.value();
    }

    @Override
    public Map<String, ?> build(TradingContext context) {
        TradingStatus status = context.getTarget().getResult().getStatus();
        if (!TradingStatus.SUCCESS.equals(status)) {
            throw new IllegalTradingStatusException(status.name());
        }
        return super.build(context);
    }

    @Override
    protected Map<String, Object> getParams(TradingContext context) {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put(WxpayParam.TRADE_NO.value(), context.getTarget().getRecordId());
        return params;
    }

    @Override
    public RequestExecutor getRequestExecutor(TradingContext context) {
        WxpayConfig config = context.getConfig();
        return new WxpayRequester(config.getGatewayUrl() + getAction(), true);
    }

    @Override
    protected TradingResult render(TradingContext context, TradingResult result) {
        super.render(context, result);
        context.getTarget().getOrder().setRefundAmount(context.getTarget().getOrder().getRefundAmount() + result.getAmount());
        persistService.updateTrade(context.getTarget());//更新原记录退款金额
        return result;
    }
}
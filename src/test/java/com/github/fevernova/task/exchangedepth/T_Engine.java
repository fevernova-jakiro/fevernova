package com.github.fevernova.task.exchangedepth;


import com.github.fevernova.framework.common.Util;
import com.github.fevernova.framework.component.DataProvider;
import com.github.fevernova.task.exchange.data.order.OrderAction;
import com.github.fevernova.task.exchange.data.order.OrderMode;
import com.github.fevernova.task.exchange.data.result.OrderMatch;
import com.github.fevernova.task.exchange.data.result.OrderMatchFactory;
import com.github.fevernova.task.exchange.data.result.ResultCode;
import com.github.fevernova.task.exchangedepth.data.DepthResult;
import com.github.fevernova.task.exchangedepth.engine.DepthEngine;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;


public class T_Engine {


    private AtomicLong seq = new AtomicLong(1);

    private DataProvider<Integer, DepthResult> provider;

    private DepthEngine depthEngine;


    @Before
    public void init() {

        this.provider = new TestProvider(true);
        this.depthEngine = new DepthEngine(100, 1000, this.provider);
    }


    @Test
    public void T_basic() {

        this.depthEngine.handle(newOrderMatch(OrderAction.BID, 10), Util.nowMS());
        this.depthEngine.handle(newOrderMatch(OrderAction.ASK, 20), Util.nowMS());

        Util.sleepSec(2);
        this.depthEngine.scan(Util.nowMS());
    }


    private OrderMatch newOrderMatch(OrderAction action, long price) {

        OrderMatch orderMatch = (OrderMatch) new OrderMatchFactory().createData();
        orderMatch.setSymbolId(1);
        orderMatch.setTimestamp(Util.nowMS());
        orderMatch.setOrderMode(OrderMode.SIMPLE);
        orderMatch.setResultCode(ResultCode.PLACE);
        orderMatch.getOrderPart1().setSequence(this.seq.getAndIncrement());
        orderMatch.getOrderPart1().setOrderAction(action);
        orderMatch.getOrderPart1().setOrderPrice(price);
        orderMatch.getOrderPart1().setOrderPriceDepthSize(1000);
        orderMatch.getOrderPart1().setOrderPriceOrderCount(3);
        return orderMatch;
    }

}

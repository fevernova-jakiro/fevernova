package com.github.fevernova.task.markettracing;


import com.github.fevernova.task.exchange.data.cmd.OrderCommandType;
import com.github.fevernova.task.markettracing.data.CandleMessage;
import com.github.fevernova.task.markettracing.data.order.DTOrder;
import com.github.fevernova.task.markettracing.data.order.OrderType;
import com.github.fevernova.task.markettracing.data.order.SLOrder;
import com.github.fevernova.task.markettracing.engine.TracingEngine;
import com.github.fevernova.task.markettracing.engine.struct.DTFactory;
import com.github.fevernova.task.markettracing.engine.struct.DTOrderBook;
import com.github.fevernova.task.markettracing.engine.struct.SLFactory;
import com.github.fevernova.task.markettracing.engine.struct.SLOrderBook;
import org.junit.Before;
import org.junit.Test;


public class T_TracingEngine {


    private TestProvider provider;

    private TracingEngine<SLOrderBook, SLOrder> slEngine;

    private TracingEngine<DTOrderBook, DTOrder> dtEngine;


    @Before
    public void init() {

        this.provider = new TestProvider(true);
        this.slEngine = new TracingEngine<>(new SLFactory(), this.provider);
        this.dtEngine = new TracingEngine<>(new DTFactory(), this.provider);
    }


    @Test
    public void T_SL() {

        CandleMessage cm1 = new CandleMessage(1, 1d, 1d, 1d, 1d, 1d, 1d, 1L, 0L, 1L, 1L);
        CandleMessage cm2 = new CandleMessage(1, 1d, 2d, 1d, 2d, 2d, 2d, 2L, 0L, 2L, 2L);
        CandleMessage cm3 = new CandleMessage(1, 1d, 3d, 1d, 3d, 3d, 3d, 3L, 0L, 3L, 3L);
        CandleMessage cm4 = new CandleMessage(1, 1d, 3d, 1d, 2d, 4d, 4d, 4L, 0L, 4L, 4L);
        this.slEngine.handleCandle(cm1);
        this.slEngine.handleOrder(1, new SLOrder(OrderCommandType.PLACE_ORDER, 1L, OrderType.UP, 1L, 1L, 3d));
        this.slEngine.handleCandle(cm2);
        this.slEngine.handleCandle(cm3);
        this.slEngine.handleCandle(cm4);
    }


    @Test
    public void T_DT() {

        CandleMessage cm1 = new CandleMessage(1, 1d, 1d, 1d, 1d, 1d, 1d, 1L, 0L, 1L,1L);
        CandleMessage cm2 = new CandleMessage(1, 1d, 2d, 1d, 2d, 2d, 2d, 2L, 0L, 2L,2L);
        CandleMessage cm3 = new CandleMessage(1, 1d, 3d, 1d, 3d, 3d, 3d, 3L, 0L, 3L,3L);
        CandleMessage cm4 = new CandleMessage(1, 1d, 3d, 1d, 2d, 4d, 4d, 4L, 0L, 4L,4L);
        this.dtEngine.handleCandle(cm1);
        this.dtEngine.handleOrder(1, new DTOrder(OrderCommandType.PLACE_ORDER, 1L, OrderType.RETRACEMENT, 1L, 1L, 0d, 1d));
        this.dtEngine.handleCandle(cm2);
        this.dtEngine.handleCandle(cm3);
        this.dtEngine.handleCandle(cm4);
    }
}

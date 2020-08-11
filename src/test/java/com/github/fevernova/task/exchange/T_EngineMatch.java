package com.github.fevernova.task.exchange;


import com.github.fevernova.Common;
import com.github.fevernova.framework.common.Util;
import com.github.fevernova.framework.common.context.GlobalContext;
import com.github.fevernova.framework.common.context.TaskContext;
import com.github.fevernova.task.exchange.data.cmd.OrderCommand;
import com.github.fevernova.task.exchange.data.cmd.OrderCommandType;
import com.github.fevernova.task.exchange.data.order.OrderAction;
import com.github.fevernova.task.exchange.data.order.OrderMode;
import com.github.fevernova.task.exchange.data.order.OrderType;
import com.github.fevernova.task.exchange.engine.OrderBooksEngine;
import org.junit.Before;
import org.junit.Test;


public class T_EngineMatch extends T_Engine {


    @Before
    public void init() {

        GlobalContext globalContext = Common.createGlobalContext();
        TaskContext taskContext = Common.createTaskContext();
        orderBooksEngine = new OrderBooksEngine(globalContext, taskContext);
        provider = new TestProvider(true);
    }


    @Test
    public void T_placeGTC() {

        OrderCommand cmd1 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.BID, OrderType.GTC, OrderMode.SIMPLE);
        cmd1.setPrice(10);
        cmd1.setSize(100);
        parser(cmd1);
        check(1, 0, 1, 0L);

        OrderCommand cmd2 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.ASK, OrderType.GTC, OrderMode.SIMPLE);
        cmd2.setPrice(9);
        cmd2.setSize(50);
        parser(cmd2);
        check(1, 0, 3, 9L);

        OrderCommand cmd3 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.ASK, OrderType.GTC, OrderMode.SIMPLE);
        cmd3.setPrice(10);
        cmd3.setSize(50);
        parser(cmd3);
        check(0, 0, 5, 10L);
    }


    @Test
    public void T_placeIOC() {

        OrderCommand cmd1 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.BID, OrderType.GTC, OrderMode.SIMPLE);
        cmd1.setPrice(10);
        cmd1.setSize(100);
        parser(cmd1);
        check(1, 0, 1, 0L);

        OrderCommand cmd2 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.ASK, OrderType.IOC, OrderMode.SIMPLE);
        cmd2.setPrice(10);
        cmd2.setSize(50);
        parser(cmd2);
        check(1, 0, 3, 10L);

        OrderCommand cmd3 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.ASK, OrderType.IOC, OrderMode.SIMPLE);
        cmd3.setPrice(10);
        cmd3.setSize(100);
        parser(cmd3);
        check(0, 0, 6, 10L);

        OrderCommand cmd4 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.ASK, OrderType.IOC, OrderMode.SIMPLE);
        cmd4.setPrice(10);
        cmd4.setSize(100);
        parser(cmd4);
        check(0, 0, 8, 10L);
    }


    @Test
    public void T_cancelGTC() {

        OrderCommand cmd1 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.BID, OrderType.GTC, OrderMode.SIMPLE);
        cmd1.setPrice(10);
        cmd1.setSize(100);
        parser(cmd1);
        check(1, 0, 1, 0L);

        cmd1.setOrderCommandType(OrderCommandType.CANCEL_ORDER);
        cmd1.setTimestamp(Util.nowMS());
        parser(cmd1);
        check(0, 0, 2, 0L);
    }


    @Test
    public void T_placeFOK() {

        OrderCommand cmd1 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.BID, OrderType.GTC, OrderMode.SIMPLE);
        cmd1.setPrice(10);
        cmd1.setSize(100);
        parser(cmd1);
        check(1, 0, 1, 0L);

        OrderCommand cmd2 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.ASK, OrderType.FOK, OrderMode.SIMPLE);
        cmd2.setPrice(10);
        cmd2.setSize(50);
        parser(cmd2);
        check(1, 0, 3, 10L);

        OrderCommand cmd3 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.ASK, OrderType.FOK, OrderMode.SIMPLE);
        cmd3.setPrice(10);
        cmd3.setSize(100);
        parser(cmd3);
        check(1, 0, 4, 10L);
    }


    @Test
    public void T_placePostOnly() {

        OrderCommand cmd1 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.BID, OrderType.GTC, OrderMode.SIMPLE);
        cmd1.setPrice(10);
        cmd1.setSize(100);
        parser(cmd1);
        check(1, 0, 1, 0L);

        OrderCommand cmd2 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.ASK, OrderType.POSTONLY, OrderMode.SIMPLE);
        cmd2.setPrice(10);
        cmd2.setSize(50);
        parser(cmd2);
        check(1, 0, 2, 0L);

        OrderCommand cmd3 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.BID, OrderType.POSTONLY, OrderMode.SIMPLE);
        cmd3.setPrice(11);
        cmd3.setSize(50);
        parser(cmd3);
        check(2, 0, 3, 0L);
    }


    @Test
    public void T_condition2Simple() {

        OrderCommand cmd1 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.BID, OrderType.GTC, OrderMode.SIMPLE);
        cmd1.setPrice(10);
        cmd1.setSize(100);
        parser(cmd1);
        check(1, 0, 1, 0L);

        OrderCommand cmd2 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.ASK, OrderType.GTC, OrderMode.SIMPLE);
        cmd2.setPrice(9);
        cmd2.setSize(100);
        parser(cmd2);
        check(0, 0, 3, 9L);

        OrderCommand cmd3 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.BID, OrderType.GTC, OrderMode.CONDITION_UP);
        cmd3.setPrice(10);
        cmd3.setTriggerPrice(9);
        cmd3.setSize(100);
        parser(cmd3);
        check(1, 0, 5, 9L);

        OrderCommand cmd4 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.BID, OrderType.GTC, OrderMode.CONDITION_UP);
        cmd4.setPrice(8);
        cmd4.setTriggerPrice(10);
        cmd4.setSize(100);
        parser(cmd4);
        check(1, 0, 6, 9L);

        OrderCommand cmd5 = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.ASK, OrderType.GTC, OrderMode.SIMPLE);
        cmd5.setPrice(10);
        cmd5.setSize(100);
        parser(cmd5);
        check(1, 0, 9, 10L);
    }


    //@Test
    public void T_performance() {

        ((TestProvider) provider).setPrint(false);

        OrderCommand bidCMD = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.BID, OrderType.GTC, OrderMode.SIMPLE);
        bidCMD.setPrice(10);
        bidCMD.setSize(100);

        OrderCommand askCMD = buildCMD(OrderCommandType.PLACE_ORDER, OrderAction.ASK, OrderType.GTC, OrderMode.SIMPLE);
        askCMD.setPrice(10);
        askCMD.setSize(100);

        Common.warmup();

        long st = Util.nowMS();
        int count = 0;
        for (int i = 0; i < 1_0000; i++) {
            for (int m = 1; m < 101; m++) {
                bidCMD.setPrice(m);
                askCMD.setPrice(101 - m);
                for (int x = 0; x < 100; x++) {
                    count++;
                    parser(bidCMD);
                    parser(askCMD);
                    bidCMD.setOrderId(bidCMD.getOrderId() + 2);
                    askCMD.setOrderId(askCMD.getOrderId() + 2);
                }
            }
        }
        long et = Util.nowMS();
        System.out.println(et - st);
        System.out.println(count * 2);
    }

}

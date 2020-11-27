package com.github.fevernova.task.exchange.engine;


import com.github.fevernova.framework.common.context.ContextObject;
import com.github.fevernova.framework.common.context.GlobalContext;
import com.github.fevernova.framework.common.context.TaskContext;
import com.github.fevernova.framework.component.DataProvider;
import com.github.fevernova.task.exchange.data.cmd.OrderCommand;
import com.github.fevernova.task.exchange.data.result.OrderMatch;
import com.github.fevernova.task.exchange.data.result.ResultCode;
import com.google.common.collect.Maps;
import lombok.Getter;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.apache.commons.lang3.Validate;

import java.util.Map;


public final class OrderBooksEngine extends ContextObject implements WriteBytesMarshallable, ReadBytesMarshallable {


    public static final String CONS_NAME = "OrderBooksEngine";

    @Getter
    private Map<Integer, OrderBooks> symbols;

    private OrderBooks lastOrderBooks;


    public OrderBooksEngine(GlobalContext globalContext, TaskContext taskContext) {

        super(globalContext, taskContext);
        this.symbols = Maps.newHashMap();
    }


    public void placeOrder(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider) {

        OrderBooks orderBooks = getOrderBooks(orderCommand);
        orderBooks.place(orderCommand, provider);
    }


    public void cancelOrder(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider) {

        OrderBooks orderBooks = getOrderBooks(orderCommand);
        orderBooks.cancel(orderCommand, provider);
    }


    public void placeConditionOrder(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider) {

        OrderBooks orderBooks = getOrderBooks(orderCommand);
        orderBooks.placeCondition(orderCommand, provider);
    }


    public void cancelConditionOrder(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider) {

        OrderBooks orderBooks = getOrderBooks(orderCommand);
        orderBooks.cancelCondition(orderCommand, provider);
    }


    public void locateMatchPrice(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider) {

        OrderBooks orderBooks = getOrderBooks(orderCommand);
        orderBooks.locateMatchPrice(orderCommand, provider);
    }


    public void heartBeat(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider) {

        OrderBooks orderBooks = getOrderBooks(orderCommand);
        orderBooks.command2result(orderCommand, provider, ResultCode.HEARTBEAT);
    }


    private OrderBooks getOrderBooks(OrderCommand orderCommand) {

        int symbolId = orderCommand.getSymbolId();
        if (this.lastOrderBooks != null && this.lastOrderBooks.getSymbolId() == symbolId) {
            return this.lastOrderBooks;
        }
        this.lastOrderBooks = this.symbols.get(symbolId);
        if (this.lastOrderBooks == null) {
            this.lastOrderBooks = new OrderBooks(symbolId);
            this.symbols.put(symbolId, this.lastOrderBooks);
        }
        return this.lastOrderBooks;
    }


    @Override public void readMarshallable(BytesIn bytes) throws IORuntimeException {

        Validate.isTrue(bytes.readInt() == 0);
        SerializationUtils.readIntMap(bytes, this.symbols, bytesIn -> new OrderBooks(bytesIn));
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeInt(0);
        SerializationUtils.writeIntMap(bytes, this.symbols);
    }
}

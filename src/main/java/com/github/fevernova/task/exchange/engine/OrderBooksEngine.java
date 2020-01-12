package com.github.fevernova.task.exchange.engine;


import com.github.fevernova.framework.common.context.ContextObject;
import com.github.fevernova.framework.common.context.GlobalContext;
import com.github.fevernova.framework.common.context.TaskContext;
import com.github.fevernova.framework.component.DataProvider;
import com.github.fevernova.task.exchange.SerializationUtils;
import com.github.fevernova.task.exchange.data.cmd.OrderCommand;
import com.github.fevernova.task.exchange.data.result.DepthRecords;
import com.github.fevernova.task.exchange.data.result.OrderMatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;

import java.util.List;
import java.util.Map;


public final class OrderBooksEngine extends ContextObject implements WriteBytesMarshallable, ReadBytesMarshallable {


    public static final String CONS_NAME = "OrderBooksEngine";

    private Map<Integer, OrderBooks> symbols;

    private OrderBooks lastOrderBooks;


    public OrderBooksEngine(GlobalContext globalContext, TaskContext taskContext) {

        super(globalContext, taskContext);
        this.symbols = Maps.newHashMap();
    }


    public void placeOrder(OrderCommand orderCommand, DataProvider<Long, OrderMatch> provider) {

        OrderBooks orderBooks = getOrderBooks(orderCommand);
        orderBooks.place(orderCommand, provider);
    }


    public void cancelOrder(OrderCommand orderCommand, DataProvider<Long, OrderMatch> provider) {

        OrderBooks orderBooks = getOrderBooks(orderCommand);
        orderBooks.cancel(orderCommand, provider);
    }


    private OrderBooks getOrderBooks(OrderCommand orderCommand) {

        int symbolId = orderCommand.getSymbolId();

        if (this.lastOrderBooks != null && this.lastOrderBooks.getSymbolId() == symbolId) {
            return this.lastOrderBooks;
        }
        OrderBooks orderBooks = this.symbols.get(symbolId);
        if (orderBooks == null) {
            orderBooks = new OrderBooks(symbolId);
            this.symbols.put(symbolId, orderBooks);
            this.lastOrderBooks = orderBooks;
        }
        return orderBooks;
    }


    public List<DepthRecords> dumpDepth() {

        final List<DepthRecords> result = Lists.newArrayListWithCapacity(this.symbols.size());
        this.symbols.entrySet().forEach(entry -> result.add(new DepthRecords(entry.getValue(), 1000)));
        return result;
    }


    @Override public void readMarshallable(BytesIn bytes) throws IORuntimeException {

        this.symbols = SerializationUtils.readIntHashMap(bytes, bytesIn -> new OrderBooks(bytesIn));
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        SerializationUtils.writeIntHashMap(this.symbols, bytes);
    }
}

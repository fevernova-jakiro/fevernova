package com.github.fevernova.task.exchange.engine;


import com.github.fevernova.framework.common.structure.queue.LinkedQueue;
import com.github.fevernova.framework.component.DataProvider;
import com.github.fevernova.task.exchange.data.Sequence;
import com.github.fevernova.task.exchange.data.cmd.OrderCommand;
import com.github.fevernova.task.exchange.data.order.*;
import com.github.fevernova.task.exchange.data.order.condition.ConditionOrder;
import com.github.fevernova.task.exchange.data.order.condition.ConditionOrderArray;
import com.github.fevernova.task.exchange.data.result.OrderMatch;
import com.github.fevernova.task.exchange.data.result.ResultCode;
import com.github.fevernova.task.exchange.data.uniq.UniqIdFilter;
import com.github.fevernova.task.exchange.engine.struct.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;


@Getter
@Slf4j
public final class OrderBooks implements WriteBytesMarshallable {


    private final int symbolId;

    private final Sequence sequence = new Sequence();

    private final Books askBooks = new AskBooks();

    private final Books bidBooks = new BidBooks();

    private final UniqIdFilter uniqIdFilter = new UniqIdFilter(60_000L, 5);

    private final ConditionBooks upBooks = new UpConditionBooks();

    private final ConditionBooks downBooks = new DownConditionBooks();

    @Setter
    private long lastMatchPrice = 0L;


    public OrderBooks(int symbolId) {

        this.symbolId = symbolId;
    }


    public OrderBooks(BytesIn bytes) {

        this.symbolId = bytes.readInt();
        this.lastMatchPrice = bytes.readLong();
        this.sequence.set(bytes.readLong());
        this.askBooks.readMarshallable(bytes);
        this.bidBooks.readMarshallable(bytes);
        this.uniqIdFilter.readMarshallable(bytes);
        this.upBooks.readMarshallable(bytes);
        this.downBooks.readMarshallable(bytes);
    }


    public void place(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider) {

        place(orderCommand, provider, true, null);
    }


    private void place(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider, boolean withUniq, LinkedQueue<ConditionOrder> queue) {

        if (withUniq && !this.uniqIdFilter.unique(orderCommand.getTimestamp(), orderCommand.getOrderId())) {
            log.error("place duplicate orderId : {} .", orderCommand.getOrderId());
            return;
        }

        Books thisBooks = OrderAction.ASK == orderCommand.getOrderAction() ? this.askBooks : this.bidBooks;
        Books thatBooks = OrderAction.ASK == orderCommand.getOrderAction() ? this.bidBooks : this.askBooks;

        if (OrderType.FOK == orderCommand.getOrderType() && !thatBooks.canMatchAll(orderCommand)) {
            command2result(orderCommand, provider, ResultCode.CANCEL_FOK);
            return;
        }

        if (OrderType.POSTONLY == orderCommand.getOrderType() && !thatBooks.newEdgePrice(orderCommand.getPrice())) {
            command2result(orderCommand, provider, ResultCode.CANCEL_POSTONLY);
            return;
        }

        Order order = thisBooks.place(orderCommand, provider, this.sequence);
        matchOrders(provider, orderCommand.getTimestamp(), orderCommand.getOrderAction());

        if (order.needIOCClear()) {
            thisBooks.cancel(orderCommand, provider, this.sequence, ResultCode.CANCEL_IOC);
        }

        thisBooks.handleLazy();

        if (this.upBooks.getOrderArray() != null || this.downBooks.getOrderArray() != null) {
            scanConditionBooks(orderCommand.getTimestamp(), provider, queue);
        }
    }


    private void matchOrders(DataProvider<Integer, OrderMatch> provider, long timestamp, OrderAction driverAction) {

        while (!this.askBooks.newEdgePrice(this.bidBooks.getPrice())) {
            //限价撮合的定价逻辑
            if (this.askBooks.getPrice() == this.bidBooks.getPrice()) {
                this.lastMatchPrice = this.askBooks.getPrice();
            } else if (this.lastMatchPrice <= this.askBooks.getPrice()) {
                this.lastMatchPrice = this.askBooks.getPrice();
            } else if (this.lastMatchPrice >= this.bidBooks.getPrice()) {
                this.lastMatchPrice = this.bidBooks.getPrice();
            }

            OrderArray bidOrderArray = this.bidBooks.getOrderArray();
            OrderArray askOrderArray = this.askBooks.getOrderArray();

            bidOrderArray.meet(this.sequence, askOrderArray, this.symbolId, this.lastMatchPrice, provider, timestamp, driverAction);

            this.bidBooks.adjustByOrderArray(bidOrderArray);
            this.askBooks.adjustByOrderArray(askOrderArray);
        }
    }


    public void cancel(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider) {

        Books books = OrderAction.ASK == orderCommand.getOrderAction() ? this.askBooks : this.bidBooks;
        if (!books.cancel(orderCommand, provider, this.sequence, ResultCode.CANCEL)) {
            command2result(orderCommand, provider, ResultCode.CANCEL_NOTFOUND);
        }
    }


    public void locateMatchPrice(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider) {

        this.lastMatchPrice = orderCommand.getPrice();
        if (this.upBooks.getOrderArray() != null || this.downBooks.getOrderArray() != null) {
            scanConditionBooks(orderCommand.getTimestamp(), provider, null);
        }
    }


    public void placeCondition(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider) {

        if (!this.uniqIdFilter.unique(orderCommand.getTimestamp(), orderCommand.getOrderId())) {
            return;
        }
        ConditionBooks books = OrderMode.CONDITION_UP == orderCommand.getOrderMode() ? this.upBooks : this.downBooks;
        books.place(orderCommand, provider, this.sequence);

        scanConditionBooks(orderCommand.getTimestamp(), provider, null);
    }


    private void scanConditionBooks(long timestamp, DataProvider<Integer, OrderMatch> provider, LinkedQueue<ConditionOrder> queue) {

        boolean isRoot = (queue == null);
        queue = isRoot ? new LinkedQueue<>() : queue;

        ConditionOrder order;
        ConditionOrderArray orderArray;
        while ((orderArray = this.upBooks.getOrderArray()) != null && !this.upBooks.newEdgePrice(this.lastMatchPrice)) {
            while ((order = orderArray.getQueue().poll()) != null) {
                queue.offer(order);
            }
            this.upBooks.adjust(orderArray, true);
        }
        while ((orderArray = this.downBooks.getOrderArray()) != null && !this.downBooks.newEdgePrice(this.lastMatchPrice)) {
            while ((order = orderArray.getQueue().poll()) != null) {
                queue.offer(order);
            }
            this.downBooks.adjust(orderArray, true);
        }

        if (isRoot && queue.size() > 0) {
            ConditionOrder tmp;
            while ((tmp = queue.poll()) != null) {
                OrderCommand cmd = new OrderCommand();
                cmd.from(this.symbolId, tmp, timestamp);
                place(cmd, provider, false, queue);
            }
        }
    }


    public void cancelCondition(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider) {

        ConditionBooks books = OrderMode.CONDITION_UP == orderCommand.getOrderMode() ? this.upBooks : this.downBooks;
        books.cancel(orderCommand, provider, this.sequence);
    }


    public void command2result(OrderCommand orderCommand, DataProvider<Integer, OrderMatch> provider, ResultCode resultCode) {

        OrderMatch orderMatch = provider.feedOne(orderCommand.getSymbolId());
        orderMatch.from(this.sequence, orderCommand, resultCode);
        provider.push();
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeInt(this.symbolId);
        bytes.writeLong(this.lastMatchPrice);
        bytes.writeLong(this.sequence.get());
        this.askBooks.writeMarshallable(bytes);
        this.bidBooks.writeMarshallable(bytes);
        this.uniqIdFilter.writeMarshallable(bytes);
        this.upBooks.writeMarshallable(bytes);
        this.downBooks.writeMarshallable(bytes);
    }
}

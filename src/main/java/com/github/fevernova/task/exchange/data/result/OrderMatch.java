package com.github.fevernova.task.exchange.data.result;


import com.github.fevernova.framework.common.data.Data;
import com.github.fevernova.task.exchange.data.Sequence;
import com.github.fevernova.task.exchange.data.cmd.OrderCommand;
import com.github.fevernova.task.exchange.data.order.Order;
import com.github.fevernova.task.exchange.data.order.OrderAction;
import com.github.fevernova.task.exchange.data.order.OrderArray;
import com.github.fevernova.task.exchange.data.order.OrderMode;
import com.github.fevernova.task.exchange.data.order.condition.ConditionOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;


@Getter
@Setter
@ToString
public class OrderMatch implements Data {


    private int symbolId;

    private long timestamp;

    private OrderMode orderMode;

    private ResultCode resultCode;

    private OrderPart orderPart1;

    private OrderPart orderPart2;

    private MatchPart matchPart;


    protected OrderMatch() {

    }


    //PLACE OR CANCEL
    public void from(Sequence sequence, OrderCommand orderCommand, Order order, OrderArray orderArray, ResultCode resultCode) {

        this.symbolId = orderCommand.getSymbolId();
        this.timestamp = orderCommand.getTimestamp();
        this.orderMode = orderCommand.getOrderMode();
        this.resultCode = resultCode;

        this.orderPart1.setSequence(sequence.getAndIncrement());
        this.orderPart1.setOrderId(order.getOrderId());
        this.orderPart1.setUserId(order.getUserId());
        this.orderPart1.setOrderAction(orderArray.getOrderAction());
        this.orderPart1.setOrderType(order.getOrderType());
        this.orderPart1.setOrderPrice(orderArray.getPrice());
        this.orderPart1.setOrderPriceDepthSize(orderArray.getSize());
        this.orderPart1.setOrderPriceOrderCount(orderArray.getQueue().size());
        this.orderPart1.setOrderTotalSize(order.getRemainSize() + order.getFilledSize());
        this.orderPart1.setOrderAccFilledSize(order.getFilledSize());
        this.orderPart1.setOrderVersion(order.getVersion());

        this.orderPart2.clearData();
        this.matchPart.clearData();
    }


    //FOK CANCEL or POSTONLY CANCEL or HEARTBEAT
    public void from(Sequence sequence, OrderCommand orderCommand, ResultCode resultCode) {

        this.symbolId = orderCommand.getSymbolId();
        this.timestamp = orderCommand.getTimestamp();
        this.orderMode = orderCommand.getOrderMode();
        this.resultCode = resultCode;

        this.orderPart1.setSequence(sequence.getAndIncrement());
        this.orderPart1.setOrderId(orderCommand.getOrderId());
        this.orderPart1.setUserId(orderCommand.getUserId());
        this.orderPart1.setOrderAction(orderCommand.getOrderAction());
        this.orderPart1.setOrderType(orderCommand.getOrderType());
        this.orderPart1.setOrderPrice(orderCommand.getPrice());
        this.orderPart1.setOrderPriceDepthSize(-1L);
        this.orderPart1.setOrderPriceOrderCount(-1);
        this.orderPart1.setOrderTotalSize(orderCommand.getSize());
        this.orderPart1.setOrderAccFilledSize(0L);
        this.orderPart1.setOrderVersion(1);

        this.orderPart2.clearData();
        this.matchPart.clearData();
    }


    //MATCH
    public void from(Sequence sequence, int symbolId, Order order, Order thatOrder, OrderArray orderArray, OrderArray thatOrderArray,
                     long matchPrice, long matchSize, long timestamp, OrderAction driverAction) {

        this.symbolId = symbolId;
        this.timestamp = timestamp;
        this.orderMode = OrderMode.SIMPLE;
        this.resultCode = ResultCode.MATCH;

        this.orderPart1.setSequence(sequence.getAndIncrement());
        this.orderPart1.setOrderId(order.getOrderId());
        this.orderPart1.setUserId(order.getUserId());
        this.orderPart1.setOrderAction(orderArray.getOrderAction());
        this.orderPart1.setOrderType(order.getOrderType());
        this.orderPart1.setOrderPrice(orderArray.getPrice());
        this.orderPart1.setOrderPriceDepthSize(orderArray.getSize());
        this.orderPart1.setOrderPriceOrderCount(orderArray.getQueue().size());
        this.orderPart1.setOrderTotalSize(order.getRemainSize() + order.getFilledSize());
        this.orderPart1.setOrderAccFilledSize(order.getFilledSize());
        this.orderPart1.setOrderVersion(order.getVersion());

        this.orderPart2.setSequence(sequence.getAndIncrement());
        this.orderPart2.setOrderId(thatOrder.getOrderId());
        this.orderPart2.setUserId(thatOrder.getUserId());
        this.orderPart2.setOrderAction(thatOrderArray.getOrderAction());
        this.orderPart2.setOrderType(thatOrder.getOrderType());
        this.orderPart2.setOrderPrice(thatOrderArray.getPrice());
        this.orderPart2.setOrderPriceDepthSize(thatOrderArray.getSize());
        this.orderPart2.setOrderPriceOrderCount(thatOrderArray.getQueue().size());
        this.orderPart2.setOrderTotalSize(thatOrder.getRemainSize() + thatOrder.getFilledSize());
        this.orderPart2.setOrderAccFilledSize(thatOrder.getFilledSize());
        this.orderPart2.setOrderVersion(thatOrder.getVersion());

        this.matchPart.setMatchPrice(matchPrice);
        this.matchPart.setMatchSize(matchSize);
        this.matchPart.setDriverAction(driverAction);
    }


    //Condition PLACE OR CANCEL
    public void from(Sequence sequence, OrderCommand orderCommand, ConditionOrder order, ResultCode resultCode) {

        this.symbolId = orderCommand.getSymbolId();
        this.timestamp = orderCommand.getTimestamp();
        this.orderMode = orderCommand.getOrderMode();
        this.resultCode = resultCode;

        this.orderPart1.setSequence(sequence.getAndIncrement());
        this.orderPart1.setOrderId(order.getOrderId());
        this.orderPart1.setUserId(order.getUserId());
        this.orderPart1.setOrderAction(order.getOrderAction());
        this.orderPart1.setOrderType(order.getOrderType());
        this.orderPart1.setOrderPrice(order.getPrice());
        this.orderPart1.setOrderPriceDepthSize(-1L);
        this.orderPart1.setOrderPriceOrderCount(0);
        this.orderPart1.setOrderTotalSize(order.getSize());
        this.orderPart1.setOrderAccFilledSize(0L);
        this.orderPart1.setOrderVersion(order.getVersion());

        this.orderPart2.clearData();
        this.matchPart.clearData();
    }


    public void from(byte[] bytes) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte version = byteBuffer.get();
        Validate.isTrue(version == 0);
        this.symbolId = byteBuffer.getInt();
        this.timestamp = byteBuffer.getLong();
        this.orderMode = OrderMode.of(byteBuffer.get());
        this.resultCode = ResultCode.of(byteBuffer.getShort());

        this.orderPart1.from(byteBuffer);

        if (ResultCode.MATCH == this.resultCode) {
            this.orderPart2.from(byteBuffer);
            this.matchPart.from(byteBuffer);
        }
    }


    @Override public byte[] getBytes() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(16 + 66 + (ResultCode.MATCH == this.resultCode ? 83 : 0));
        byteBuffer.put((byte) 0);
        byteBuffer.putInt(this.symbolId);
        byteBuffer.putLong(this.timestamp);
        byteBuffer.put(this.orderMode.code);
        byteBuffer.putShort((short) this.resultCode.code);

        this.orderPart1.getBytes(byteBuffer);

        if (ResultCode.MATCH == this.resultCode) {
            this.orderPart2.getBytes(byteBuffer);
            this.matchPart.getBytes(byteBuffer);
        }
        return byteBuffer.array();
    }


    @Override public void clearData() {

        //this.symbolId = 0;
        //this.timestamp = 0L;
        //this.orderMode = null;
        //this.resultCode = null;
        //this.orderPart1.clearData();
        //this.orderPart2.clearData();
        //this.matchPart.clearData();
    }


    public long maxSeq() {

        return Math.max(this.orderPart1.getSequence(), this.orderPart2.getSequence());
    }
}

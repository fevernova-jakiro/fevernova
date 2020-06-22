package com.github.fevernova.task.exchange.data.cmd;


import com.github.fevernova.task.exchange.data.order.OrderAction;
import com.github.fevernova.task.exchange.data.order.OrderMode;
import com.github.fevernova.task.exchange.data.order.OrderType;
import com.github.fevernova.task.exchange.data.order.condition.ConditionOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;


@Getter
@Setter
@ToString
public class OrderCommand {


    public static final int BYTE_SIZE = 57;

    private OrderMode orderMode;

    private OrderCommandType orderCommandType;

    private long orderId;

    private int symbolId;

    private long userId;

    private long timestamp;

    private OrderAction orderAction;

    private OrderType orderType;

    private long price;

    private long size;

    private long triggerPrice;


    public void from(int symbolId, ConditionOrder order, long timestamp) {

        this.orderMode = OrderMode.SIMPLE;
        this.orderCommandType = OrderCommandType.PLACE_ORDER;
        this.orderId = order.getOrderId();
        this.symbolId = symbolId;
        this.userId = order.getUserId();
        this.timestamp = timestamp;
        this.orderAction = order.getOrderAction();
        this.orderType = order.getOrderType();
        this.price = order.getPrice();
        this.size = order.getSize();
        this.triggerPrice = 0L;
    }


    public void from(byte[] bytes) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte version = byteBuffer.get();
        Validate.isTrue(version == 0);
        this.orderMode = OrderMode.of(byteBuffer.get());
        this.orderCommandType = OrderCommandType.of(byteBuffer.get());
        this.orderId = byteBuffer.getLong();
        this.symbolId = byteBuffer.getInt();
        this.userId = byteBuffer.getLong();
        this.timestamp = byteBuffer.getLong();
        this.orderAction = OrderAction.of(byteBuffer.get());
        this.orderType = OrderType.of(byteBuffer.get());
        this.price = byteBuffer.getLong();
        this.size = byteBuffer.getLong();
        this.triggerPrice = byteBuffer.getLong();
    }


    public byte[] to() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(BYTE_SIZE);
        byteBuffer.put((byte) 0);
        byteBuffer.put(this.orderMode.code);
        byteBuffer.put(this.orderCommandType.code);
        byteBuffer.putLong(this.orderId);
        byteBuffer.putInt(this.symbolId);
        byteBuffer.putLong(this.userId);
        byteBuffer.putLong(this.timestamp);
        byteBuffer.put(this.orderAction.code);
        byteBuffer.put(this.orderType.code);
        byteBuffer.putLong(this.price);
        byteBuffer.putLong(this.size);
        byteBuffer.putLong(this.triggerPrice);
        return byteBuffer.array();
    }
}

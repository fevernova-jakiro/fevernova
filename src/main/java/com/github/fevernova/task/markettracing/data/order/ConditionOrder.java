package com.github.fevernova.task.markettracing.data.order;


import com.github.fevernova.task.exchange.data.cmd.OrderCommandType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;


@Getter
@NoArgsConstructor
public class ConditionOrder implements WriteBytesMarshallable {


    protected OrderCommandType commandType;

    protected Long orderId;

    protected OrderType orderType;

    protected Long userId;

    protected Long timestamp;


    public ConditionOrder(OrderCommandType commandType, Long orderId, OrderType orderType, Long userId, Long timestamp) {

        this.commandType = commandType;
        this.orderId = orderId;
        this.orderType = orderType;
        this.userId = userId;
        this.timestamp = timestamp;
    }


    public ConditionOrder(BytesIn bytes) {

        this.orderId = bytes.readLong();
        this.orderType = OrderType.of(bytes.readByte());
        this.userId = bytes.readLong();
        this.timestamp = bytes.readLong();
    }


    public void from(byte[] bytes) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte version = byteBuffer.get();
        Validate.isTrue(version == 0);
        this.commandType = OrderCommandType.of(byteBuffer.get());
        this.orderId = byteBuffer.getLong();
        this.orderType = OrderType.of(byteBuffer.get());
        this.userId = byteBuffer.getLong();
        this.timestamp = byteBuffer.getLong();
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeLong(this.orderId);
        bytes.writeByte(this.orderType.code);
        bytes.writeLong(this.userId);
        bytes.writeLong(this.timestamp);
    }
}

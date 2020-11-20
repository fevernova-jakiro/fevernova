package com.github.fevernova.task.markettracing.data.order;


import com.github.fevernova.task.exchange.data.cmd.OrderCommandType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;


@Getter
@NoArgsConstructor
public class SLOrder extends ConditionOrder {


    private Double triggerPrice;


    public SLOrder(OrderCommandType commandType, Long orderId, OrderType orderType, Long userId, Long timestamp, Double triggerPrice) {

        super(commandType, orderId, orderType, userId, timestamp);
        this.triggerPrice = triggerPrice;
    }


    public SLOrder(BytesIn bytes) {

        super(bytes);
        this.triggerPrice = bytes.readDouble();
    }


    @Override public void from(byte[] bytes) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte version = byteBuffer.get();
        Validate.isTrue(version == 0);
        super.commandType = OrderCommandType.of(byteBuffer.get());
        super.orderId = byteBuffer.getLong();
        super.orderType = OrderType.of(byteBuffer.get());
        super.userId = byteBuffer.getLong();
        super.timestamp = byteBuffer.getLong();
        this.triggerPrice = byteBuffer.getDouble();
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        super.writeMarshallable(bytes);
        bytes.writeDouble(this.triggerPrice);
    }
}

package com.github.fevernova.task.markettracing.data.order;


import com.github.fevernova.task.exchange.data.cmd.OrderCommandType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;


@Getter
@NoArgsConstructor
public class DTOrder extends ConditionOrder {


    @Setter
    private double polarPrice;

    private double deltaPrice;


    public DTOrder(OrderCommandType commandType, Long orderId, OrderType orderType, Long userId, Long timestamp, Double polarPrice, Double deltaPrice) {

        super(commandType, orderId, orderType, userId, timestamp);
        this.polarPrice = polarPrice;
        this.deltaPrice = deltaPrice;
    }


    public DTOrder(BytesIn bytes) {

        super(bytes);
        this.polarPrice = bytes.readDouble();
        this.deltaPrice = bytes.readDouble();
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
        this.polarPrice = byteBuffer.getDouble();
        this.deltaPrice = byteBuffer.getDouble();
    }


    public double getTriggerPrice() {

        if (OrderType.RETRACEMENT == this.orderType) {
            return this.polarPrice - this.deltaPrice;
        } else {
            return this.polarPrice + this.deltaPrice;
        }
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        super.writeMarshallable(bytes);
        bytes.writeDouble(this.polarPrice);
        bytes.writeDouble(this.deltaPrice);
    }
}

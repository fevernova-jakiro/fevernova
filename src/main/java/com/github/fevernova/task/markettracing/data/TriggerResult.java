package com.github.fevernova.task.markettracing.data;


import com.github.fevernova.framework.common.data.Data;
import com.github.fevernova.task.markettracing.data.order.ConditionOrder;
import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;


@Getter
@ToString
public class TriggerResult implements Data {


    private int pairCodeId;

    private long orderId;

    private long userId;


    protected TriggerResult() {

    }


    public void from(int pairCodeId, ConditionOrder conditionOrder) {

        this.pairCodeId = pairCodeId;
        this.orderId = conditionOrder.getOrderId();
        this.userId = conditionOrder.getUserId();
    }


    @Override public byte[] getBytes() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(21);
        byteBuffer.put((byte) 0);
        byteBuffer.putInt(this.pairCodeId);
        byteBuffer.putLong(this.orderId);
        byteBuffer.putLong(this.userId);
        return new byte[0];
    }


    @Override public void clearData() {

        this.pairCodeId = 0;
        this.orderId = 0L;
        this.userId = 0L;
    }


    @Override public long getTimestamp() {

        return 0;
    }
}

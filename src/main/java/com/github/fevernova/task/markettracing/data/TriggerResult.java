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

    private Status status;


    protected TriggerResult() {

    }


    public void from(int pairCodeId, ConditionOrder conditionOrder, Status status) {

        this.pairCodeId = pairCodeId;
        this.orderId = conditionOrder.getOrderId();
        this.userId = conditionOrder.getUserId();
        this.status = status;
    }


    @Override public byte[] getBytes() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(22);
        byteBuffer.put((byte) 0);
        byteBuffer.putInt(this.pairCodeId);
        byteBuffer.putLong(this.orderId);
        byteBuffer.putLong(this.userId);
        byteBuffer.put(this.status.code);
        return byteBuffer.array();
    }


    @Override public void clearData() {

        this.pairCodeId = 0;
        this.orderId = 0L;
        this.userId = 0L;
        this.status = null;
    }


    @Override public long getTimestamp() {

        return 0;
    }


    public enum Status {
        PLACE(0), CANCEL(1), TRIGGER(2);

        private byte code;


        Status(int code) {

            this.code = (byte) code;
        }


        public static Status of(int code) {

            switch (code) {
                case 0:
                    return PLACE;
                case 1:
                    return CANCEL;
                case 2:
                    return TRIGGER;
                default:
                    throw new IllegalArgumentException("unknown Status :" + code);
            }
        }

    }
}
